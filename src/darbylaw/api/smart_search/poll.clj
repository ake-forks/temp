(ns darbylaw.api.smart-search.poll
  (:require
    [chime.core :as ch]
    [clojure.tools.logging :as log]
    [xtdb.api :as xt]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.base64 :refer [decode-base64]]
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
    (doseq [[{check-id :xt/id :keys [ssid]}
             {:keys [case-id reference]}]
            checks]
      (try
        (let [smartdoc-resp (ss-api/get-doccheck ssid)
              updated-data (-> smartdoc-resp
                               (get-in [:body :data :attributes])
                               (select-keys [:status :result]))

              smartdoc-filename
              (when (terminal? (:status updated-data))
                (let [export-resp (ss-api/export-pdf-base64 ssid)
                      base64 (get-in export-resp [:body :data :attributes :base64])
                      bytes (decode-base64 base64)

                      document-id (random-uuid)
                      filename (str reference ".identity.smartdoc-report." document-id ".pdf")]
                  (doc-store/store
                    (str case-id "/" filename)
                    bytes
                    {:content-type "application/pdf"})
                  filename))]
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              ;; TODO: Add to case history
              (tx-fns/set-values check-id 
                                 (cond-> updated-data
                                   smartdoc-filename (assoc :report smartdoc-filename))))))
        (catch Exception e
          (log/error e "Failed syncing check ssid:" ssid))))))


(defn sync-job
  [xtdb-node]
  (log/info "Syncing smart search job")
  (try
    (sync! xtdb-node)
    (finally
      (log/info "Finished syncing smart search job"))))

(mount/defstate smart-search-sync-job
  :start (ch/chime-at
           (rest (ch/periodic-seq (Instant/now) (Duration/ofMinutes 10)))
           (fn [_time]
             (sync-job xtdb-node/xtdb-node)))
  :stop (.close smart-search-sync-job))
