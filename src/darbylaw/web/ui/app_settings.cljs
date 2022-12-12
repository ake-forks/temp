(ns darbylaw.web.ui.app-settings
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]))

(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (assoc db :settings response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load
  (fn [_ _]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri "/api/settings"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-sub ::settings
  (fn [db _]
    (:settings db)))

(rf/reg-event-fx ::merge-settings--success
  (fn [_ [_ _]]
      {:dispatch [::load]}))

(rf/reg-event-fx ::merge-settings--failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::merge-settings
  (fn [_ [_ settings]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri "/api/settings"
        :params settings
        :on-success [::merge-settings--success]
        :on-failure [::merge-settings--failure]})}))
