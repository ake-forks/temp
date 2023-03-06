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
    [chime.core :as ch]
    [darbylaw.api.bank-notification.mailing-fetch :refer [fetch-letters-to-send]]
    [darbylaw.api.bank-notification.mailing-watchdog :as mailing-watchdog]
    [darbylaw.api.bank-notification.mailing-config :refer [mailing-upload-time]])
  (:import (java.time Period ZoneId ZonedDateTime)
           java.time.temporal.ChronoUnit
           (clojure.lang ExceptionInfo)))

(defn disabled? [xtdb-node]
  (-> (settings/fetch-settings xtdb-node)
    :post-letters-disabled?))

(defn upload-mail! [xtdb-node real|fake letters]
  (when (and (seq letters)
             (doc-store/available?)
             (mailing/available? real|fake))
    (let [letter-ids (map :xt/id letters)]
      (mailing-watchdog/assert-no-duplicates xtdb-node letter-ids))
    (let [n-letters (count letters)
          max-batch-size (-> config/config :mailing-service :max-batch-size)]
      (assert (<= n-letters max-batch-size)
              (format "Suspiciously high number of letters to send (%d/%d)" n-letters max-batch-size))
      (log/infof "Uploading %d/%d letters for mailing" n-letters max-batch-size))
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
                  (concat
                    (tx-fns/set-value letter-id [:upload-state] :uploaded)
                    (mailing-watchdog/watch letter-id)))
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
