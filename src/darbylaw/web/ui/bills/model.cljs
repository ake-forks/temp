(ns darbylaw.web.ui.bills.model
  (:require [re-frame.core :as rf]
            [darbylaw.api.bill.data :as bills-data]))

(rf/reg-sub ::companies
  (fn [_]
    bills-data/companies))

(rf/reg-sub ::all-company-ids
  :<- [::companies]
  (fn [companies]
    (map :id companies)))

(defn label-fn [coll]
  (let [label-by-id (into {} (map (juxt :id :common-name) coll))]
    (fn [id]
      (or (get label-by-id (keyword id))
          (name id)))))

(rf/reg-sub ::company-id->label
  :<- [::companies]
  (fn [companies]
    (label-fn companies)))

(rf/reg-sub ::councils
  (fn [_]
    bills-data/councils))

(rf/reg-sub ::all-council-ids
  :<- [::councils]
  (fn [councils]
    (map :id councils)))

(rf/reg-sub ::council-id->label
  :<- [::councils]
  (fn [councils]
    (label-fn councils)))

(rf/reg-sub ::bill-types
  (fn [_]
    (for [[name {:keys [label]}] bills-data/bill-types]
      {:name name
       :label label})))
