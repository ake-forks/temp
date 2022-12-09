(ns darbylaw.api.util.xtdb
  (:require [xtdb.api :as xt])
  (:import (java.util Date)))

(def assoc-in--txn-fn
  '(fn [ctx eid ks v]
     (when-let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (assoc-in e ks v)]])))

(defn assoc-in--txns [eid m v]
  [[::xt/put {:xt/id ::assoc-in
              :xt/fn assoc-in--txn-fn}]
   [::xt/fn ::assoc-in eid m v]])

; TODO: check for txn errors
(defn exec-tx [xtdb-node tx-ops]
  (xt/await-tx xtdb-node
    (xt/submit-tx xtdb-node
      tx-ops)))

(defn exec-txn
  "Deprecated name. Use `exec-tx`"
  [xtdb-node tx-ops]
  (exec-tx xtdb-node tx-ops))

(defn fetch-one [xt-results]
  (assert (= 1 (count xt-results))
    (str "Expected one result, got " (count xt-results)))
  (ffirst xt-results))

(defn now []
  (Date.))