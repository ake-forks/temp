(ns darbylaw.api.util.tx-fns
  (:require [xtdb.api :as xt]))

(defn invoke [id args f]
  [[::xt/put {:xt/id id
              :xt/fn f}]
   (into [::xt/fn id] args)])

(defn assoc* [eid ks x]
  (invoke ::assoc [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (assoc-in e ks x)]]))))

(defn into* [eid ks coll]
  (invoke ::into [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (fnil #(into % coll) []))]]))))

(defn conj-unique [eid ks x]
  (invoke ::conj-unique [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (fnil
                                      (fn [v]
                                        (if (some #{x} v) v (conj v x)))
                                      []))]]))))
