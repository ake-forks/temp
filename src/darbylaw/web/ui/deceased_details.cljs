(ns darbylaw.web.ui.deceased-details
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [kee-frame.core :as kf]
            [darbylaw.web.ui.deceased-details-form :as form]))

(rf/reg-event-fx ::load!
  (fn [{:keys [db]} [_ case-id]]
    {:db (dissoc db :current-case)
     :dispatch [::case-model/load-case! case-id]}))

(kf/reg-controller ::load
  {:params (fn [route-data]
             (when (= :deceased-details (-> route-data :data :name))
               (-> route-data :path-params :case-id)))
   :start (fn [_context case-id]
            [::load! case-id])})

(kf/reg-controller ::dispose
  {:params (fn [route-data]
             (when (= :deceased-details (-> route-data :data :name))
               true))
   :start (fn [& _])
   :stop (fn [& _]
           (form/dispose))})

(defn panel []
  (if-let [current-case @(rf/subscribe [::case-model/current-case])]
    [form/panel :edit
     {:initial-values (:deceased current-case)}]
    [mui/circular-progress]))