(ns darbylaw.api.bank-notification.mailing-job
  (:require
    [xtdb.api :as xt]
    [darbylaw.config :as config]
    [darbylaw.api.util.xtdb :as xt-util]
    [clojure.tools.logging :as log]
    [darbylaw.api.bank-notification.letter-store :as letter-store]
    [darbylaw.doc-store :as doc-store]
    [darbylaw.api.util.files :as files-util :refer [with-delete]]
    [darbylaw.api.services.mailing :as mailing]
    [mount.core :as mount]
    [darbylaw.xtdb-node :as xtdb-node]
    [darbylaw.api.settings :as settings]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [chime.core :as ch])
  (:import (java.time LocalTime Period ZoneId ZonedDateTime)
           java.time.temporal.ChronoUnit
           (clojure.lang ExceptionInfo)))

(defn disabled? [xtdb-node]
  (-> (settings/fetch-settings xtdb-node)
    :post-letters-disabled?))

(defn fetch-letters-to-send [xtdb-node real|fake]
  (let [send-action (case real|fake
                      :real :send
                      :fake :fake-send)]
    (->> (xt/q (xt/db xtdb-node)
           '{:find [(pull letter [*])]
             :where [[letter :type :probate.bank-notification-letter]
                     [letter :send-action send-action]
                     (not [letter :upload-state])]
             :in [send-action]}
           send-action)
         (map first))))

(comment
  (fetch-letters-to-send darbylaw.xtdb-node/xtdb-node :fake))

(defn upload-mail! [xtdb-node real|fake letters]
  (when (and (seq letters)
             (doc-store/available?)
             (mailing/available? real|fake))
    (assert (<= (count letters)
                (-> config/config :mailing-service :max-batch-size))
            "Only a max of :max-batch-size letters can be uploaded at once")
    (doseq [letter-data letters]
      (let [{:keys [case-id bank-id]
             letter-id :xt/id} letter-data
            tx (xt-util/exec-tx xtdb-node
                 (concat
                   [[::xt/match (:xt/id letter-data) letter-data]]
                   (tx-fns/set-value letter-id [:upload-state] :uploading)))
            own? (xt/tx-committed? xtdb-node tx)]
        (when own?
          (try
            (with-delete [temp-file (files-util/create-temp-file letter-id ".pdf")]
              (try
                (doc-store/fetch-to-file
                  (letter-store/s3-key case-id bank-id letter-id ".pdf")
                  temp-file)
                (let [remote-filename (str letter-id ".pdf")]
                  (mailing/post-letter real|fake (.getCanonicalPath temp-file) remote-filename)
                  (log/debug "Uploaded file for mailing: " remote-filename))
                (xt-util/exec-tx xtdb-node
                  (tx-fns/set-value letter-id [:upload-state] :uploaded))
                (catch ExceptionInfo exc
                  (if (= (-> exc ex-data :error) ::doc-store/not-found)
                    (xt-util/exec-tx xtdb-node
                      (tx-fns/set-value letter-id [:upload-state] :not-found))
                    (throw exc)))))
            (catch Exception exc
              (log/warn exc "Error while uploading letter; will retry later.")
              (xt-util/exec-tx xtdb-node
                (tx-fns/set-value letter-id [:upload-state] :retry-upload)))))))))

(defn upload-mail-job [xtdb-node]
  (if (disabled? xtdb-node)
    (log/warn "Uploading letters is disabled!")
    (do
      (log/info "Starting uploading letters to external mailing system...")
      (try
        (upload-mail! xtdb-node :real (fetch-letters-to-send xtdb-node :real))
        (finally
          (log/info "Finished uploading letters to external mailing system."))))))

(mount/defstate mailing-upload-time
  :start (-> config/config :mailing-service :upload-time LocalTime/parse))

(mount/defstate mailing-upload-job
  :start (ch/chime-at
           (ch/periodic-seq
             (-> mailing-upload-time
               ^ZonedDateTime (.adjustInto (ZonedDateTime/now (ZoneId/of "Europe/London")))
               .toInstant)
             (Period/ofDays 1))
           (fn [_time]
             (upload-mail-job xtdb-node/xtdb-node)))
  :stop (.close mailing-upload-job))
