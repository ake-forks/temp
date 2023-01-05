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
  (assert (string? (:username user)))
  (put-with-tx-data
    (merge
      (dissoc event-data :case-id :user)
      {:xt/id (random-uuid)
       :type :event
       :subject-type :probate.case
       :event/case case-id
       :event event
       :by (:username user)})))

(comment
  (put-event {:event :testing-event
              :case-id (random-uuid)
              :user {:username "me"}}))

