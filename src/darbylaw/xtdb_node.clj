(ns darbylaw.xtdb-node
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :refer [pprint]]
    [xtdb.api :as xt]
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [darbylaw.config :as config]))


;; >> Postgres

(defn xtdb-postgres-config
  "Index store: RocksDB
   Doc store:   Postgres
   Tx store:    Postgres"
  [data-dir config]
  (let [{:keys [db-spec]} config]
    {:xtdb.jdbc/connection-pool
     {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
      :pool-opts {}
      :db-spec db-spec}

     :xtdb/tx-log
     {:xtdb/module 'xtdb.jdbc/->tx-log
      :connection-pool :xtdb.jdbc/connection-pool}

     :xtdb/document-store
     {:xtdb/module 'xtdb.jdbc/->document-store
      :connection-pool :xtdb.jdbc/connection-pool}

     :xtdb/index-store
     {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                 :db-dir (io/file data-dir "indexes")}}}))

     ;; TODO: Metrics
     ;:xtdb.metrics.csv/reporter
     ;{:output-file "xtdb-metrics.csv"}}))



;; >> RocksDB

(defn xtdb-rocksdb-config
  [data-dir _config]
  {:xtdb/tx-log
   {:kv-store
    {:xtdb/module 'xtdb.rocksdb/->kv-store
     :sync? true
     :db-dir (io/file data-dir "rocksdb-tx-log")}}

   :xtdb/document-store
   {:kv-store
    {:xtdb/module 'xtdb.rocksdb/->kv-store
     :sync? true
     :db-dir (io/file data-dir "rocksdb-doc-store")}}

   :xtdb/index-store
   {:kv-store
    {:xtdb/module 'xtdb.rocksdb/->kv-store
     :sync? true
     :db-dir (io/file data-dir "rocksdb-indexes")}}})



;; >> In-memory

(defn xtdb-in-memory-config
  [_data-dir _config]
  {})



;; >> XTDB Config

(def xtdb-data-dir ".xtdb")

(defn xtdb-config
  [{:keys [node-type] :as config}]
  (let [config-fn
        (case node-type
          :postgres xtdb-postgres-config
          :rocksdb xtdb-rocksdb-config
          :in-memory xtdb-in-memory-config
          (throw
            (Exception. 
              (str "Unknown XTDB node type in config: "
                   (pr-str node-type)))))]
    (config-fn xtdb-data-dir config)))



;; >> Actually run the node

(defstate xtdb-node
  :start (-> config/config :database xtdb-config xt/start-node)
  :stop (.close xtdb-node))

(comment
  ;; delete everything
  (let [res (xt/q (xt/db xtdb-node)
                  '{:find [id]
                    :where [[id :type anything]]})
        ids (map first res)]
    (->> res
         (map first)
         (mapv (fn [id] [::xt/delete id]))
         (xt/submit-tx xtdb-node)))

  (xt/entity (xt/db xtdb-node) :testing))
