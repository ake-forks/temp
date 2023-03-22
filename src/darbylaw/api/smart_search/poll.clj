(ns darbylaw.api.smart-search.poll
  (:require
    [chime.core :as ch]
    [clojure.tools.logging :as log]
    [darbylaw.api.smart-search.config :as ss-config]
    [xtdb.api :as xt]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.base64 :refer [decode-base64]]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.doc-store :as doc-store]
    [mount.core :as mount]
    [darbylaw.xtdb-node :as xtdb-node]
    [darbylaw.api.smart-search.api :as ss-api])
  (:import (java.time Duration Instant)))

(def checks-to-sync
  '{:find [(pull check [*])
           (pull case [(:xt/id {:as :case-id})
                       :reference])]
    :where [[check :check-type :smartdoc]
            [check :status status]
            [(not= status "processed")]
            [(not= status "failed")]
            [(not= status "invalid")]
            [check :probate.identity-check.smartdoc/case case-id]
            [case :xt/id case-id]]})

(comment
  (xt/q (xt/db xtdb-node/xtdb-node)
    checks-to-sync
    #_
    '{:find [(pull check [*])]
      :where [[check :check-type :smartdoc]
              [check :status status]]}))

(def terminal? #{"processed" "failed" "invalid"})

(defn sync!
  [xtdb-node]
  (let [checks (xt/q (xt/db xtdb-node) checks-to-sync)]
    (log/info "Syncing" (count checks) "checks")
    (doseq [[{check-id :xt/id :keys [ssid links-self]}
             {:keys [case-id reference]}]
            checks]
      (try
        (let [env (ss-config/link->env links-self)
              ss-client (ss-api/client-for-env env)
              smartdoc-resp (ss-client (ss-api/get-doccheck-request ssid))
              updated-data (-> smartdoc-resp
                               (get-in [:body :data :attributes])
                               (select-keys [:status :result]))]
          (if-not (terminal? (:status updated-data))
            ;; If we've not reached a terminal state then just update the state in the database
            (xt-util/exec-tx-or-throw xtdb-node
              (tx-fns/set-values check-id updated-data))
            ;; Otherwise:
            ;; - Upload the report to the document store
            ;; - Update the state in the database
            ;; - Update the case history
            (let [export-resp (ss-client (ss-api/export-pdf-base64-request ssid))
                  base64 (get-in export-resp [:body :data :attributes :base64])
                  bytes (decode-base64 base64)

                  document-id (random-uuid)
                  filename (str reference ".identity.smartdoc-report." document-id ".pdf")]
              (doc-store/store
                (str case-id "/" filename)
                bytes
                {:content-type "application/pdf"})
              (xt-util/exec-tx-or-throw xtdb-node
                (concat
                  (tx-fns/set-values check-id (assoc updated-data :report filename))
                  (case-history/put-event2 {:case-id case-id
                                            :subject :probate.case.identity-checks.smartdoc
                                            :op :updated}))))))
        (catch Exception e
          (log/error e "Failed syncing check ssid:" ssid))))))


(defn sync-job
  [xtdb-node]
  (log/info "Syncing smart search job")
  (try
    (sync! xtdb-node)
    (finally
      (log/info "Finished syncing smart search job"))))

(comment
  (sync! xtdb-node/xtdb-node))

(mount/defstate smart-search-sync-job
  :start (ch/chime-at
           (rest (ch/periodic-seq (Instant/now) (Duration/ofMinutes 10)))
           (fn [_time]
             (sync-job xtdb-node/xtdb-node)))
  :stop (.close smart-search-sync-job))
