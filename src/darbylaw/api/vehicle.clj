(ns darbylaw.api.vehicle
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.vehicle.data :as data]))


;; >> Handlers

(defn upsert [{:keys [xtdb-node user parameters]}]
  (let [{:keys [case-id vehicle-id]} (:path parameters)
        insert? (nil? vehicle-id)
        vehicle-id (if insert? (random-uuid) vehicle-id)
        vehicle-data (:multipart parameters)]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/put (merge {:xt/id vehicle-id
                           :type :probate.vehicle
                           :probate.vehicle/case case-id}
                          vehicle-data)]]
        (tx-fns/append-unique case-id [:vehicles] [vehicle-id])
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.vehicle
           :op (if insert? :added :updated)})))
    {:status http/status-200-ok
     :body {:id vehicle-id}}))

(comment
  (upsert
    {:xtdb-node darbylaw.xtdb-node/xtdb-node
     :user {:username "osm"}
     :parameters {:path {:case-id (parse-uuid "c68c5adc-e4f1-4159-a9b1-0ab1de98c85c")
                         :vehicle-id (parse-uuid "41fa2bbf-5650-4d7b-b46b-fd140aafcc44")}
                  :multipart {:registration-number "CUA 12345"
                              :description "Silver Ford Fiesta"
                              :estimated-value "123.12"}}}))



;; >> Routes

(defn routes []
  ["/case/:case-id"
   ["/vehicle"
    {:post {:handler upsert
            :parameters {:path [:map [:case-id :uuid]]
                         :multipart data/schema}}}]
   ["/vehicle/:vehicle-id"
    {:post {:handler upsert
            :parameters {:path [:map
                                [:case-id :uuid]
                                [:vehicle-id :uuid]]
                         :multipart data/schema}}}]])
