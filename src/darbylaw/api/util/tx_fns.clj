(ns darbylaw.api.util.tx-fns
  (:require [xtdb.api :as xt]))

(defn invoke [id args f]
  [[::xt/put {:xt/id id
              :xt/fn f}]
   (into [::xt/fn id] args)])

(defn set-value [eid ks x]
  (assert (sequential? ks))
  (invoke ::set-value [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (assoc-in e ks x)]]))))

(defn set-values
  ([eid ks m]
   (invoke ::set-values [eid ks m]
     '(fn [ctx eid ks m]
        (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
          [[::xt/put (if (empty? ks)
                       (merge e m)
                       (update-in e ks merge m))]]))))
  ([eid m]
   (set-values eid [] m)))

(defn append [eid ks coll]
  (assert (sequential? ks))
  (assert (sequential? coll))
  (invoke ::append [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (-> (fn [v]
                                          (let [v (if (vector? v) v (vec v))]
                                            (into v coll)))
                                      (fnil [])))]]))))

(defn append-unique [eid ks coll]
  (assert (sequential? ks))
  (assert (sequential? coll))
  (invoke ::append-unique [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (-> (fn [v]
                                          (let [coll (remove (set v) coll)
                                                v (if (vector? v) v (vec v))]
                                            (into v coll)))
                                      (fnil [])))]]))))

(defn remove-unique [eid ks coll]
  (assert (sequential? ks))
  (assert (sequential? coll))
  (invoke ::remove [eid ks coll]
    '(fn [ctx eid ks coll]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (update-in e ks (-> #(->> % (remove (set coll)) vec)
                                        (fnil [])))]]))))

(defn put-unique [e]
  (assert (:xt/id e))
  (invoke ::put-unique [e]
    '(fn [ctx e]
       (let [e* (xtdb.api/entity (xtdb.api/db ctx) (:xt/id e))]
         (when-not e* [[::xt/put e]])))))

(defn assert-exists [eid]
  (invoke ::assert-exists [eid]
    '(fn [ctx eid]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         (if (some? e)
           []
           false)))))

(defn assert-nil [eid ks]
  (assert (sequential? ks))
  (invoke ::assert-nil [eid ks]
    '(fn [ctx eid ks]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         (if (nil? (get-in e ks))
           []
           false)))))

(defn assert-some [eid ks]
  (assert (sequential? ks))
  (invoke ::assert-some [eid ks]
    '(fn [ctx eid ks]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         (if (some? (get-in e ks))
           []
           false)))))

(defn assert-equals [eid ks x]
  (assert (sequential? ks))
  (invoke ::assert-some [eid ks x]
    '(fn [ctx eid ks x]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         (if (= (get-in e ks) x)
           []
           false)))))
