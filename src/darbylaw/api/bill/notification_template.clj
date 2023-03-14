(ns darbylaw.api.bill.notification-template
  (:require
    [darbylaw.api.bill.data :as bill-data]
    [clojure.string :as str]
    [stencil.api :as stencil]
    [clojure.java.io :as io]
    [java-time.api :as jt]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.dates :as date-util]
    [xtdb.api :as xt]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.xtdb-node :as node]
    [mount.core :as mount]))

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
          (assoc :date (date-util/long-date (jt/local-date) false))
          (assoc :property property-data)
          (assoc-in [:deceased :date-of-death] (date-util/long-date-from-string
                                                 (:date-of-death (:deceased case-data))
                                                 false)))
        (case asset-type
          :utility
          (merge
            {:accounts (mapv (fn [entry] {:account-number (:account-number entry)
                                          :meter-reading (:meter-readings entry)}) asset-data)}
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
    #uuid"2546a9ef-b4fc-4056-b73e-3ac70c632a67"
    :boost-energy
    #uuid"e7bd1719-e757-4ea8-8237-1627cb4f80fa")

  (mapv first (apply xt/q (xt/db node/xtdb-node)
                (utility-query #uuid"2546a9ef-b4fc-4056-b73e-3ac70c632a67"
                  :boost-energy #uuid"e7bd1719-e757-4ea8-8237-1627cb4f80fa")))


  ())




(mount/defstate templates
  :start {:utility (stencil/prepare (io/resource "darbylaw/templates/utility-notification.docx"))
          :council-tax (stencil/prepare (io/resource "darbylaw/templates/council-notification.docx"))})

(defn render-docx [bill-type template-data file]
  (stencil/render!
    (get templates bill-type)
    template-data
    :output file
    :overwrite? true))