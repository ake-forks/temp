(ns darbylaw.api.bill.notification-template
  (:require
    [darbylaw.api.bill.data :as bill-data]
    [clojure.string :as str]
    [stencil.api :as stencil]
    [clojure.java.io :as io]
    [darbylaw.api.util.data :as data-util]
    [xtdb.api :as xt]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.xtdb-node :as node]
    [mount.core :as mount])
  (:import (java.time LocalDate)))

(defn generate-utility-address [company]
  (let [data (bill-data/get-company-info company)
        vector (vector
                 (:address-1 data)
                 (:address-2 data)
                 (:town data)
                 (:county data)
                 (:postcode data))
        address (str/join "\n"
                  (remove str/blank? vector))]
    (merge
      {:org-name (:common-name data)}
      (if (str/blank? address)
        {:no-address "No address data found. Please download and edit letter before sending."}
        {:org-address address}))))


(defn utility-query [case-id utility-company property-id]
  [{:find '[(pull utility [*])]
    :where '[[utility :type :probate.utility]
             [utility :probate.utility/case case-id]
             [utility :utility-company utility-company]
             [utility :property property-id]]
    :in '[case-id utility-company property-id]}
   case-id utility-company property-id])
(defn generate-council-address [council]
  (let [data (bill-data/get-council-info council)
        vector (vector
                 (:address-1 data)
                 (:address-2 data)
                 (:town data)
                 (:county data)
                 (:postcode data))]
    {:org-name (:common-name data)
     :org-address (str/join "\n"
                    (remove str/blank? vector))}))

(defn council-query [case-id council property-id]
  [{:find '[(pull council-tax [*])]
    :where '[[council-tax :type :probate.council-tax]
             [council-tax :probate.council-tax/case case-id]
             [council-tax :council council]
             [council-tax :property property-id]]
    :in '[case-id council property-id]}
   case-id council property-id])

(defn case-query [case-id property-id]
  [{:find '[(pull case [:reference
                        :deceased {(:probate.deceased/_case {:as :deceased
                                                             :cardinality :one})
                                   [:forename :surname :date-of-death]}])
            (pull property [:address])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [property :xt/id property-id]]
    :in '[case-id property-id]}
   case-id property-id])



(defn get-letter-data [xtdb-node asset-type case-id institution property-id]
  (let [database (xt/db xtdb-node)
        [case-data property-data] (xt-util/fetch-one
                                    (apply xt/q database
                                      (case-query case-id property-id)))
        asset-data (mapv first (apply xt/q database
                                 (case asset-type
                                   :utility (utility-query case-id institution property-id)
                                   :council-tax (council-query case-id institution property-id))))]

    (data-util/keys-to-camel-case
      (merge
        (-> case-data
          (assoc :date (.toString (LocalDate/now)))
          (assoc :property property-data))
        (case asset-type
          :utility
          (merge
            {:account-number (mapv #(:account-number %) asset-data)}
            (generate-utility-address (:utility-company (first asset-data))))
          :council-tax
          (merge
            {:account-number (if-let [acc (:account-number (first asset-data))]
                               acc
                               "Unknown")}
            (generate-council-address (:council (first asset-data)))))))))

(comment
  (get-letter-data node/xtdb-node
    :utility
    #uuid"6dc1ab3a-44a6-4600-bf3a-4255271c3421"
    :swalec
    #uuid"28e725a3-d7a5-477a-a1e3-4816eb9241b3"))

(mount/defstate templates
  :start {:utility (stencil/prepare (io/resource "darbylaw/templates/utility-notification.docx"))
          :council-tax (stencil/prepare (io/resource "darbylaw/templates/council-notification.docx"))})

(defn render-docx [bill-type template-data file]
  (stencil/render!
    (get templates bill-type)
    template-data
    :output file
    :overwrite? true))