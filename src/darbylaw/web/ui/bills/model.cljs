(ns darbylaw.web.ui.bills.model
  (:require [re-frame.core :as rf]
            [darbylaw.api.bill.data :as bills-data]
            [darbylaw.api.bill.council-data :as council-data]
            [darbylaw.web.ui.case-model :as case-model]
            [medley.core :as medley]))

(rf/reg-sub ::companies
  (fn [_]
    bills-data/companies))

(rf/reg-sub ::all-company-ids
  :<- [::companies]
  (fn [companies]
    (map :id companies)))

(defn id->label-fn [coll]
  (let [label-by-id (into {} (map (juxt :id :common-name) coll))]
    (fn [id]
      (or (get label-by-id (keyword id))
          (name id)))))

(rf/reg-sub ::company-id->label
  :<- [::companies]
  (fn [companies]
    (id->label-fn companies)))

(rf/reg-sub ::councils
  (fn [_]
    council-data/councils))

(rf/reg-sub ::all-council-ids
  :<- [::councils]
  (fn [councils]
    (map :id councils)))

(rf/reg-sub ::council-id->label
  :<- [::councils]
  (fn [councils]
    (id->label-fn councils)))

(rf/reg-sub ::bill-types
  (fn [_]
    (for [[name {:keys [label]}] bills-data/bill-types]
      {:name name
       :label label})))

(rf/reg-sub ::current-bills
  :<- [::case-model/current-case]
  #(:bills %))

(rf/reg-sub ::used-billing-addresses
  :<- [::current-bills]
  (fn [bills]
    (->> bills
      (map :address)
      (filter string?)
      (distinct))))

(rf/reg-sub ::current-properties
  :<- [::case-model/current-case]
  (fn [case-data]
    (:properties case-data)))

(rf/reg-sub ::current-properties-by-id
  :<- [::current-properties]
  (fn [properties]
    (medley/index-by :id properties)))
