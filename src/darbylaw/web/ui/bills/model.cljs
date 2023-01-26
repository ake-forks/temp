(ns darbylaw.web.ui.bills.model
  (:require [re-frame.core :as rf]
            [darbylaw.api.bills.data :as bills-data]))

(rf/reg-sub ::companies
  (fn [_]
    bills-data/companies))

(rf/reg-sub ::all-company-ids
  :<- [::companies]
  (fn [companies]
    (map :id companies)))

(rf/reg-sub ::company-id->label
  :<- [::companies]
  (fn [companies]
    (let [label-by-id (into {} (map (juxt :id :common-name) companies))]
      (fn [id]
        (or (get label-by-id (keyword id))
            (name id))))))

(rf/reg-sub ::bill-types
  (fn [_]
    (for [[name {:keys [label]}] bills-data/bill-types]
      {:name name
       :label label})))
