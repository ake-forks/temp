(ns darbylaw.api.bill.notification-template
  (:require [darbylaw.api.bank-notification-template :as bank-template]
    [darbylaw.api.bill.data :as bill-data]
    [clojure.string :as str]
    [stencil.api :as stencil]
    [clojure.java.io :as io]
    [darbylaw.api.util.data :as data-util]
    [xtdb.api :as xt]
    [darbylaw.api.util.xtdb :as xt-util]
    [mount.core :as mount])
  (:import (java.time LocalDate)))

;; Bill addresses are based off bank addresses so they use the same format
(defn generate-address-vector [data]
  (bank-template/generate-address-vector :bank data))

(defn generate-mailing-address [bill-type asset-id]
  (let [asset-data (case bill-type
                     :utility (bill-data/get-company-info asset-id)
                     :council-tax (bill-data/get-council-info asset-id))
        address-vector (generate-address-vector asset-data)]
    {:org-name (:org-name asset-data)
     :org-address (->> address-vector
                       (remove str/blank?)
                       (str/join "\n"))}))

(defn bill-letter-template-query [case-id bill-type bill-id]
  [{:find '[(pull case [:reference
                        :deceased {(:probate.deceased/_case {:as :deceased
                                                             :cardinality :one})
                                   [:forename :surname :date-of-death]}])
            (pull bill [*])]
    :where ['[case :type :probate.case]
            '[case :xt/id case-id]
            '[bill :type :probate.bill]
            '[bill :xt/id bill-id]
            '[bill :probate.bill/case case-id]
            '[bill :bill-type bill-type]
            (case bill-type
              :utility '[(!= bill-type :council-tax)]
              :council-tax '[(== bill-type :council-tax)])]
    :in '[case-id bill-id]}
   case-id bill-id])

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

(defn council-template-query [case-id council-tax-id]
  [{:find '[(pull case [:reference
                        :deceased {(:probate.deceased/_case {:as :deceased
                                                             :cardinality :one})
                                   [:forename :surname :date-of-death]}])
            (pull council-tax [*])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [council-tax :type :probate.council-tax]
             [council-tax :xt/id council-tax-id]]
    :in '[case-id council-tax-id]}
   case-id council-tax-id])

(defn get-council-letter-data [xtdb-node case-id council-tax-id]
  (let [[case-data council-data] (xt-util/fetch-one
                                   (apply xt/q (xt/db xtdb-node)
                                     (council-template-query case-id council-tax-id)))
        account-info (if (str/blank? (:account-number council-data))
                       "Unknown"
                       (:account-number council-data))]

    (data-util/keys-to-camel-case
      (-> case-data
        (assoc :date (.toString (LocalDate/now)))
        (assoc :council (merge
                          (assoc council-data :account-number account-info)
                          (generate-council-address (:council council-data))))))))

(comment
  (council-template-query #uuid"2bcee42c-df67-4aff-93e1-b91002239a38" #uuid"eabeaa33-251d-4c09-a445-0df40d091a75")

  (xt-util/fetch-one
    (apply xt/q (xt/db xtdb-node)
      (council-template-query
        #uuid"2bcee42c-df67-4aff-93e1-b91002239a38"
        #uuid"eabeaa33-251d-4c09-a445-0df40d091a75")))




  (comment))




(defn letter-template-data [xtdb-node bill-type case-id bill-id]
  {:reference "12341234"}
  #_(let [[case-data bank-data]
          (xt-util/fetch-one
            (apply xt/q (xt/db xtdb-node)
                   (bill-letter-template-query case-id bill-type bill-id)))]
      (data-util/keys-to-camel-case
        (-> case-data
            (assoc :date (str (LocalDate/now)))
            (assoc bill-type (merge bank-data
                                    (generate-mailing-address bill-type bill-id)))))))

(mount/defstate templates
  :start {:utility (stencil/prepare (io/resource "darbylaw/templates/utility-notification.docx"))
          :council-tax (stencil/prepare (io/resource "darbylaw/templates/council-notification.docx"))})

(defn render-docx [bill-type template-data file]
  (stencil/render!
    (get templates bill-type)
    template-data
    :output file
    :overwrite? true))

(comment
  (def templates
    {:utility (stencil/prepare (io/resource "darbylaw/templates/utility-notification.docx"))
     :council-tax (stencil/prepare (io/resource "darbylaw/templates/council-notification.docx"))})

  (let [case-id (parse-uuid "36e6c3a9-fd66-4958-be37-2bd88aa24a17")
        ;[bill-id bill-type] [(parse-uuid "f96e912e-9fbc-49f0-832c-eb99f304162b") :council]
        [bill-id bill-type] [(parse-uuid "28ca6137-b98f-4040-811b-152dfe3bd6fc") :utility]]
    (let [file (darbylaw.api.util.files/create-temp-file "test" ".docx")]
      (render-docx bill-type
                   (letter-template-data
                     darbylaw.xtdb-node/xtdb-node
                     bill-type case-id bill-id)
                   file))
    #_
    (apply xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
           (bill-letter-template-query case-id bill-type bill-id))
    ;; TODO: Think about best way to retrieve bills
    #_
    (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
          '{:find [(pull case [{:bills [*]}])
                   (pull bill [*])]
            :where [[case :type :probate.case]
                    [case :xt/id case-id]
                    [bill :xt/id bill-id]]
            :in [case-id bill-id]}
          case-id bill-id)
    #_
    (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
      '{:find [(pull bill [*])]
        :where [[bill :type :probate.bill]
                [bill :probate.bill/case case-id]
                [bill :bill-type bill-type]
                [(== :council-tax bill-type)]]
        :in [case-id bill-id]}
      case-id bill-id)))
