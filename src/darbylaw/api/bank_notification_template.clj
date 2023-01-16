(ns darbylaw.api.bank-notification-template
  (:require
    [clojure.string :as string]
    [darbylaw.api.util.xtdb :as xt-util]
    [mount.core :as mount]
    [xtdb.api :as xt]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.bank-list :as banks]
    [darbylaw.api.buildsoc-list :as buildsocs]
    [stencil.api :as stencil]
    [clojure.java.io :as io])
  (:import (java.time LocalDate)))

(defn generate-vector [data]
  (vector
    (:address-street-no data)
    (:address-house-name data)
    (:address-line-1 data)
    (:address-line-2 data)
    (:address-line-3 data)
    (:address-town data)
    (:address-postcode data)
    (:address-county data)
    (:address-country data)))


(defn generate-mailing-address [type asset-id]
  (let [asset-data (if (= type :bank) (banks/get-bank-info asset-id))
        address-vector (generate-vector asset-data)]
    {:org-name (:org-name asset-data)
     :org-address (string/join "\n"
                    (remove string/blank? address-vector))}))

(defn generate-account-info [accounts type]
  (if (= type :bank)
    (if (:accounts-unknown accounts)
      "Account information unknown"
      (string/join "\n" (mapv (fn [acc] (str (:sort-code acc) " " (:account-number acc))) accounts)))
    (if (:accounts-unknown accounts)
      "Account information unknown"
      (string/join "\n" (mapv (fn [acc] (:roll-number acc)) accounts)))))

(defn bank-letter-template-query [case-id bank-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull bank-accounts [:accounts])
           '(pull deceased [:forename :surname :date-of-death])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [deceased :probate.deceased/case case-id]
             [bank-accounts :type :probate.bank-accounts]
             [bank-accounts :bank-id bank-id]
             [bank-accounts :case-id case-id]]
    :in '[case-id
          bank-id]}
   case-id
   bank-id])

(defn buildsoc-letter-template-query [case-id buildsoc-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull buildsoc-accounts [:accounts])
           '(pull deceased [:forename :surname :date-of-death])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [deceased :probate.deceased/case case-id]
             [buildsoc-accounts :type :probate.buildsoc-accounts]
             [buildsoc-accounts :buildsoc-id buildsoc-id]
             [buildsoc-accounts :case-id case-id]]
    :in '[case-id
          buildsoc-id]}
   case-id
   buildsoc-id])

(defn get-letter-template-data [xtdb-node bank-type case-id bank-id]
  (let [[case-data bank-data deceased-data] (xt-util/fetch-one
                                              (apply xt/q (xt/db xtdb-node)
                                                (case bank-type
                                                  :bank (bank-letter-template-query case-id bank-id)
                                                  :buildsoc (buildsoc-letter-template-query case-id bank-id))))]

    (data-util/keys-to-camel-case
      (cond
        (= bank-type :bank)
        (-> case-data
          (assoc :bank (-> bank-data
                         (assoc :name (banks/bank-label bank-id))
                         (assoc :account-string (generate-account-info (:accounts bank-data) bank-type))
                         (assoc :name (banks/bank-label bank-id))
                         (merge (generate-mailing-address bank-type bank-id))))
          (assoc :deceased deceased-data)
          (assoc :date (.toString (LocalDate/now))))
        (= bank-type :buildsoc)
        (-> case-data
          (assoc :buildsoc (-> bank-data
                             (assoc :name (buildsocs/buildsoc-label bank-id))
                             (assoc :account-string (generate-account-info (:accounts bank-data) bank-type))
                             (assoc :name (buildsocs/buildsoc-label bank-id))
                             (merge (generate-mailing-address bank-type bank-id))))
          (assoc :deceased deceased-data)
          (assoc :date (.toString (LocalDate/now))))))))


(mount/defstate templates
  :start {:bank (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx"))
          :buildsoc (stencil/prepare (io/resource "darbylaw/templates/buildsoc-notification.docx"))})

(defn render-docx [bank-type template-data file]
  (stencil/render!
    (get templates bank-type)
    template-data
    :output file
    :overwrite? true))
