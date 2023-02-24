(ns darbylaw.api.util.xtdb
  (:require [xtdb.api :as xt]
            [medley.core])
  (:import (java.util Date)))

(defn get-reference [xtdb-node case-id]
  (-> (xt/pull (xt/db xtdb-node)
        [:reference] case-id)
    :reference))

(def assoc-in--txfn
  '(fn [ctx eid ks v]
     (when-let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (assoc-in e ks v)]])))

(defn assoc-in-tx [eid ks v]
  [[::xt/put {:xt/id ::assoc-in
              :xt/fn assoc-in--txfn}]
   [::xt/fn ::assoc-in eid ks v]])

(def deep-merge--txfn
  '(fn [ctx m]
     (if-let [e (xtdb.api/entity (xtdb.api/db ctx) (:xt/id m))]
       [[::xt/put (medley.core/deep-merge e m)]]
       [[::xt/put m]])))

(defn deep-merge-tx [m]
  [[::xt/put {:xt/id ::deep-merge
              :xt/fn deep-merge--txfn}]
   [::xt/fn ::deep-merge m]])

(defn exec-tx [xtdb-node tx-ops]
  (xt/await-tx xtdb-node
    (xt/submit-tx xtdb-node
      tx-ops)))

(defn throw-if-failed-tx [xtdb-node tx-result]
  (if (xt/tx-committed? xtdb-node tx-result)
    tx-result
    (let [tx-id (:tx-id tx-result)]
      (throw (ex-info (str "Transaction failed with tx-id " tx-id)
               {::tx-id tx-id})))))

(defn exec-tx-or-throw [xtdb-node tx-ops]
  (throw-if-failed-tx xtdb-node
    (exec-tx xtdb-node tx-ops)))

(defn fetch-one [xt-results]
  (assert (= 1 (count xt-results))
    (str "Expected one result, got " (count xt-results)))
  (first xt-results))

(defn now []
  (Date.))