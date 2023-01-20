(ns darbylaw.api.bank-notification.mailing-sync-job
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [darbylaw.api.services.mailing :as mailing]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt])
  (:import (com.jcraft.jsch SftpException)))

(defn files->beans [fs]
  (->> fs
    (map (fn [f]
           (-> f
             bean
             (update :attrs bean))))))

(defn remove-dirs [fs]
  (->> fs
    (filter #(not (get-in % [:attrs :dir])))))

(defn parse-awaiting-filename [f]
  (let [[match letter-id] (re-matches #"(.+)\.pdf" f)]
   (when match
     {:letter-id letter-id})))

(def errors-dir "Errors")

(defn parse-errored-filename [f]
  (let [[match letter-id error] (re-matches #"(.+)\.(.*)\.pdf" f)]
    (when match
      {:letter-id letter-id
       :error error})))

(comment
  (parse-awaiting-filename "hola.filename.pdf")
  (parse-errored-filename "hola.filename.errored.pdf")
  (parse-awaiting-filename "hola.pdf"))

(defn digest-files [ls-entries parse-fn]
  (->> ls-entries
    (files->beans)
    (remove-dirs)
    (keep #(if-let [m (parse-fn (:filename %))]
             (merge % m)
             (log/error "Unexpected filename: " (:filename %))))))

(def letters-awaiting-send
  '{:find [(pull letter [*])]
    :where [[letter :type :probate.bank-notification-letter]
            [letter :upload-state :uploaded]
            (not [letter :send-state])]})

(defn sftp-remove--ignore-not-found [remote path]
  (try
    (mailing/run-sftp-command remote :rm path)
    (catch SftpException e
      (when-not (-> e ex-message str/lower-case (str/starts-with? "no such"))
        (throw e)))))

(defn sync! [remote xtdb-node]
  (let [; Snapshot of uploaded letters taken first, for preventing
        ; syncing letters uploaded during the execution of this procedure.
        uploaded (xt/q (xt/db xtdb-node) letters-awaiting-send)
        ; Both `ls` operations one just after another to reduce the risk of
        ; in-between changes, and have a snapshot as consistent as possible.
        awaiting-files (mailing/run-sftp-command remote :ls)
        errored-files (mailing/run-sftp-command remote :ls "Errors")

        awaiting-files (digest-files awaiting-files parse-awaiting-filename)
        errored-files (digest-files errored-files parse-errored-filename)]

    ; Process all files found in errored folder.
    ; We iterate through all errored files, so that we correct any possible
    ; misbehaviour in the remote mailing process, where a file might just disappear
    ; completely, and therefore be marked as `:sent`, but later appear in the
    ; errors folder, so it has to be corrected and marked as `:errored`.
    (doseq [{:keys [letter-id error filename]} errored-files]
      (try
        (let [tx (xt-util/exec-tx xtdb-node
                   (concat
                     (tx-fns/assert-exists letter-id)
                     (tx-fns/set-values letter-id {:send-state :error
                                                   :send-error error})))]
          (if-not (xt/tx-committed? xtdb-node tx)
            ; This can happen, as we share remote accounts among deployed
            ; environments. Therefore, just a warning.
            (log/warn "Corresponding letter not found in DB:" letter-id)
            ; File may have been removed already by a concurrent execution:
            (sftp-remove--ignore-not-found remote (str errors-dir "/" filename))))
        (catch Exception e
          (log/error e "Failed syncing errored letter file" filename))))

    ; Process all letters that are in upload-state `:uploaded`.
    ; If they are not in root nor errored, mark them as `:sent`.
    (let [uploaded-ids (map (comp :xt/id first) uploaded)
          awaiting-ids (map :letter-id (concat awaiting-files errored-files))
          sent-ids (->> uploaded-ids
                     (remove (set awaiting-ids)))]
      (doseq [sent-id sent-ids]
        (try
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              ; Sanity check; shouldn't fail:
              (tx-fns/assert-nil sent-id [:send-state])
              (tx-fns/set-values sent-id {:send-state :sent})))
          (catch Exception e
            (log/error e "Failed syncing sent letter" sent-id)))))))

(comment
  (def xtdb-node darbylaw.xtdb-node/xtdb-node)
  (letters-awaiting-send (xt/db xtdb-node))

  (xt/entity (xt/db xtdb-node)
    "000299.barclays-bank-plc.bank-notification.23b8b714-db7f-440b-8656-5d5c7ae68ea1")
  (xt/q (xt/db xtdb-node)
    '{:find [(pull letter [*])]
      :where [[letter :type :probate.bank-notification-letter]
              [letter :upload-state :uploaded]]})

  (keys xtdb-node)
  (-> xtdb-node :tx-log)

  (mailing/run-sftp-command :real :rm (str errors-dir "/.FailReason0.pdf"))

  (sync! :fake xtdb-node)
  ,)
