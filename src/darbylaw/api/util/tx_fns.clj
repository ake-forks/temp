(ns darbylaw.api.util.tx-fns
  (:require [xtdb.api :as xt]))

(defn invoke [id args f]
  [[::xt/put {:xt/id id
              :xt/fn f}]
   (into [::xt/fn id] args)])

(defn set-value [eid ks x]
  (invoke ::set-value [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (assoc-in e ks x)]]))))

(defn merge-value [eid ks x]
  (invoke ::merge-value [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks merge x)]]))))

(defn append [eid ks coll]
  (invoke ::append [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (-> (fn [v]
                                          (let [v (if (vector? v) v (vec v))]
                                            (into v coll)))
                                      (fnil [])))]]))))

(defn append-unique [eid ks coll]
  (invoke ::append-unique [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (-> (fn [v]
                                          (let [coll (remove (set v) coll)
                                                v (if (vector? v) v (vec v))]
                                            (into v coll)))
                                      (fnil [])))]]))))
