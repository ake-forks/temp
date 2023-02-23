(ns darbylaw.api.smart-search.poll
  (:require
    [chime.core :as ch]
    [clojure.tools.logging :as log]
    [xtdb.api :as xt]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [mount.core :as mount]
    [darbylaw.xtdb-node :as xtdb-node]
    [darbylaw.api.smart-search.api :as ss-api])
  (:import (java.time Duration Instant)))

(def checks-to-sync
  '{:find [(pull check [*])]
    :where [[check :type :smartdoc]
            [check :status status]
            [(not= status "processed")]
            [(not= status "failed")]
            [(not= status "invalid")]]})

(comment
  (xt/q (xt/db xtdb-node/xtdb-node)
    checks-to-sync
    #_
    '{:find [(pull check [*])]
      :where [[check :type :smartdoc]
              [check :status status]]}))

(defn sync!
  [xtdb-node]
  (let [checks (xt/q (xt/db xtdb-node) checks-to-sync)]
    (log/info "Syncing" (count checks) "checks")
    (doseq [{check-id :xt/id :keys [ssid]}
            (map first checks)]
      (try
        (let [response (ss-api/get-doccheck ssid)
              updated-data (-> response
                               (get-in [:body :data :attributes])
                               (select-keys [:status :result]))]
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              ;; TODO: Add to case history
              (tx-fns/set-values check-id updated-data))))
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
