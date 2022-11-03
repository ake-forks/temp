(ns darbylaw.web.ui.case-model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]))

(rf/reg-sub ::case-id
  :<- [::ui/path-params]
  (fn [path-params]
    (:case-id path-params)))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::nickname
  :<- [::current-case]
  #(-> % :personal-representative :forename))

(rf/reg-event-fx ::load-case!
  (fn [_ [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id)
        :on-success [::load-success]
        :on-failure [::load-failure case-id]})}))

(rf/reg-event-fx ::load-success
  (fn [{:keys [db]} [_ response]]
    {:db (assoc db :current-case response)}))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ case-id result]]
    (js/console.error "Case load failed" case-id result)))
