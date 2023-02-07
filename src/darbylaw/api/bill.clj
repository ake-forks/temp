(ns darbylaw.api.bill
  (:require [darbylaw.api.bill.data :as bill-data]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.util.xtdb :as xt-util]
            [xtdb.api :as xt]))

(def creation-schema
  (bill-data/make-bill-schema :create))

(def creation-props
  (bill-data/extract-bill-props creation-schema))

(comment
  (require '[malli.core :as malli])
  (malli.core/explain creation-schema {:bill-type #{:other}
                                       ;:issuer :utility-1
                                       :amount "10"
                                       :custom-issuer-name "hola"
                                       :custom-issuer-address "addr"
                                       :address "hey"}))

(defn handle-property [{:keys [user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property (:property body-params)]
    (cond
      (string? property)
      (let [new-property-id (random-uuid)]
        [new-property-id (concat
                           [[::xt/put {:xt/id new-property-id
                                       :type :probate.property
                                       :probate.property/case case-id
                                       :address property}]]
                           (case-history/put-event
                             {:event :properties-updated
                              :case-id case-id
                              :user user
                              :op :add
                              :event/property new-property-id}))])

      (uuid? property)
      [property (tx-fns/assert-exists property)]

      :else
      (assert false (str "Unexpected type:" (type property))))))

(defn add-bill [{:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        bill-id (random-uuid)
        [property-id property-tx] (handle-property args)
        bill-data (-> body-params
                    (select-keys creation-props)
                    (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        [[::xt/put (merge bill-data
                     {:xt/id bill-id
                      :type :probate.bill
                      :probate.bill/case case-id})]]
        (tx-fns/append case-id [:bills] [bill-id])
        (case-history/put-event {:event :bills-updated
                                 :case case-id
                                 :user user
                                 :op :add
                                 :event/bill bill-id}))))
  {:status http/status-204-no-content})

(defn routes []
  ["/case/:case-id/bill" {:post {:handler add-bill
                                 :parameters {:body creation-schema}}}])

(comment
  (require 'darbylaw.xtdb-node)
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull doc [*])]
      :where [[doc :type :probate.bill]]}))