(ns darbylaw.web.ui.bills.model
  (:require [re-frame.core :as rf]
            [darbylaw.api.bills.company-list :as company-list]))

(rf/reg-sub ::companies
  (fn [_]
    company-list/company-list))

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
