(ns darbylaw.web.ui.user-details
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [kee-frame.core :as kf]
            [darbylaw.web.ui.user-details-form :as form]))

(rf/reg-event-fx ::loaded
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc-in db [::case-loaded? case-id] true)}))

(rf/reg-event-fx ::dispose
  (fn [{:keys [db]} _]
    {:db (dissoc db ::case-loaded?)}))

(kf/reg-controller ::load-case
  {:params (fn [route-data]
             (when (= :user-details (-> route-data :data :name))
               (-> route-data :path-params :case-id)))
   :start (fn [_ case-id]
            [::case-model/load-case! case-id
             {:on-success [::loaded case-id]}])
   :stop [::dispose]})

(rf/reg-sub ::loaded?
  (fn [db [_ case-id]]
    (get-in db [::case-loaded? case-id])))

(defn user-details-panel []
  [mui/container {:max-width :sm}
   [mui/typography {:variant :h3
                    :sx {:pt 4 :pb 2}}
    "your details"]
   (let [case-id @(rf/subscribe [::case-model/case-id])]
     (if @(rf/subscribe [::loaded? case-id])
       [form/personal-info-form :edit
        {:initial-values (:personal-representative
                           @(rf/subscribe [::case-model/current-case]))}]
       [mui/circular-progress]))])
