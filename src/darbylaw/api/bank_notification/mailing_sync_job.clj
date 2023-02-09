(ns darbylaw.api.bank-notification.mailing-sync-job
  (:require
    [chime.core :as ch]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.services.mailing :as mailing]
    [darbylaw.api.bank-notification.mailing-job :as upload-job]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [mount.core :as mount]
    [xtdb.api :as xt]
    [darbylaw.xtdb-node :as xtdb-node]
    [darbylaw.api.util.dates :refer [instant->localtime]])
  (:import (com.jcraft.jsch SftpException)
           (java.time Duration Instant LocalTime ZoneId)
           java.time.temporal.ChronoUnit))

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
        errored-files (mailing/run-sftp-command remote :ls errors-dir)

        awaiting-files (digest-files awaiting-files parse-awaiting-filename)
        errored-files (digest-files errored-files parse-errored-filename)]

    ; Process all files found in errored folder.
    ; We iterate through all errored files, so that we correct any possible
    ; misbehaviour in the remote mailing process, where a file might just disappear
    ; completely, and therefore be marked as `:sent`, but later appear in the
    ; errors folder, so it has to be corrected and marked as `:errored`.
    (doseq [{:keys [letter-id error filename]} errored-files]
      (try
        (if-let [{:keys [case-id bank-id]} (xt/entity (xt/db xtdb-node) letter-id)]
          (do
            (xt-util/exec-tx-or-throw xtdb-node
              (concat
                (tx-fns/set-values letter-id {:send-state :error
                                              :send-error error
                                              :send-state-changed (xt-util/now)})
                (case-history/put-event {:event :bank-notification.letter-send-error
                                         :case-id case-id
                                         :bank-id bank-id
                                         :letter-id letter-id
                                         :send-error error})))
            (sftp-remove--ignore-not-found remote (str errors-dir "/" filename)))
          ; This can happen, as we share remote accounts among deployed
          ; environments. Therefore, just a warning.
          (log/warn "Corresponding letter not found in DB:" letter-id))
        (catch Exception e
          (log/error e "Failed syncing errored letter file" filename))))

    ; Process all letters that are in upload-state `:uploaded`.
    ; If they are not in root nor errored, mark them as `:sent`.
    (let [awaiting-ids (set (map :letter-id (concat awaiting-files errored-files)))
          sent-letters (->> uploaded
                         (map first)
                         (remove #(contains? awaiting-ids (:xt/id %))))]
      (doseq [{letter-id :xt/id
               case-id :case-id
               bank-id :bank-id} sent-letters]
        (try
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              ; Sanity check; shouldn't fail:
              (tx-fns/assert-nil letter-id [:send-state])
              (tx-fns/set-values letter-id {:send-state :sent
                                            :send-state-changed (xt-util/now)})
              (case-history/put-event {:event :bank-notification.letter-sent
                                       :case-id case-id
                                       :bank-id bank-id
                                       :letter-id letter-id})))
          (catch Exception e
            (log/error e "Failed syncing sent letter" letter-id)))))))

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

(defn sync-job [xtdb-node]
  (log/info "Starting syncing letter send results...")
  (try
    (sync! :real xtdb-node)
    (finally
      (log/info "Finished syncing letter send results."))))

(mount/defstate mailing-sync-job
  :start (ch/chime-at
           (->> (rest (ch/periodic-seq (Instant/now) (Duration/ofMinutes 10)))
             (remove (fn [instant]
                       (let [t (instant->localtime instant (ZoneId/of "Europe/London"))
                             exclusion-start
                             (.minus upload-job/mailing-upload-time
                                     65 ChronoUnit/MINUTES)
                             exclusion-end
                             (.plus upload-job/mailing-upload-time
                                    65 ChronoUnit/MINUTES)]
                         (and (.isAfter t exclusion-start)
                              (.isBefore t exclusion-end))))))
           (fn [_time]
             (sync-job xtdb-node/xtdb-node)))
  :stop (.close mailing-sync-job))

(comment
  (sync-job xtdb-node/xtdb-node)
  (clj-ssh.ssh/disconnect (:real @mailing/ssh-session-atom))

  (def fivePM (LocalTime/of 17 00 00))
  (def sixPM (LocalTime/of 18 00 00))
  (def t (instant->localtime #inst"2023-01-27T17:00:01" (ZoneId/of "Europe/London")))
  (.isBefore fivePM t)
  (and (.isBefore fivePM t) (.isBefore t sixPM)))
