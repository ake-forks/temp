(ns darbylaw.api.util.xtdb
  (:require [xtdb.api :as xt]
            [medley.core])
  (:import (java.util Date)))

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
  (first xt-results))

(defn now []
  (Date.))