(ns darbylaw.web.util.bank
  (:require
    [darbylaw.api.local-bank-data :refer [bank-list]]))

(defn get-banks []
  bank-list)

(defn get-bank-common-names []
  (map (fn [bank] (:common-name bank)) bank-list))

(defn get-bank-by-id [id]
  (first (filter (fn [bank] (= (:id bank) id)) bank-list)))

(defn get-bank-by-common-name [name]
  (first (filter (fn [bank] (= (:common-name bank) name)) bank-list)))

(comment
  (get-banks)
  (get-bank-common-names)
  (get-bank-by-id "santander")
  (get-bank-by-common-name "Halifax"))




