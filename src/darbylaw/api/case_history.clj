(ns darbylaw.api.case-history
  (:require [darbylaw.api.util.tx-fns :as tx-fns]
            [xtdb.api :as xt]))

(defn put-with-tx-data [m]
  (tx-fns/invoke ::put-with-tx-data [m]
    '(fn [ctx m]
       (let [tx (xtdb.api/indexing-tx ctx)]
         [[::xt/put (assoc m
                      :timestamp (::xt/tx-time tx)
                      :tx-id (::xt/tx-id tx))]]))))

(defn put-event [{:keys [event case-id user] :as event-data}]
  (assert (keyword? event))
  (assert (uuid? case-id))
  (put-with-tx-data
    (merge
      (dissoc event-data :case-id :user)
      {:xt/id (random-uuid)
       :type :event
       :subject-type :probate.case
       :event/case case-id
       :event event}
      (when user
        (assert (string? (:username user)))
        {:by (:username user)}))))

(defn put-event2 [{:keys [subject case-id user op] :as event-data}]
  (assert (keyword? subject))
  (assert (uuid? case-id))
  (assert (keyword? op))
  (put-with-tx-data
    (merge
      (dissoc event-data :case-id :user)
      {:xt/id (random-uuid)
       :type :event
       :event/case case-id}
      (when user
        (assert (string? (:username user)))
        {:by (:username user)}))))

(comment
  (put-event {:event :testing-event
              :case-id (random-uuid)
              :user {:username "me"}})

  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull ev [*]) t]
      :where [[ev :event/case]
              [ev :timestamp t]]
      :order-by [[t :asc]]}))

