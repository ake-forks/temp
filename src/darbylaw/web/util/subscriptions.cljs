(ns darbylaw.web.util.subscriptions
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]))


(rf/reg-sub ::route-params
  (fn [db _]
    (:path-params (:kee-frame/route db))))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-event-fx
  ::load-success
  (fn [{:keys [db]} [_ response]]
    {:db (assoc db :current-case response)}))

(rf/reg-event-db
  ::load-failure
  (fn [db [_ case-id result]]
    (assoc db :failure-http-result result :case-id case-id)))

(rf/reg-event-fx ::load!
  (fn [_ [_ case-id]]
    {:dispatch [::get-case! case-id]}))

(rf/reg-event-fx ::get-case!
  (fn [_ [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id)
        :on-success [::load-success]
        :on-failure [::load-failure case-id]})}))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))

(rf/reg-event-db
  ::show-bank-modal
  (fn [db value]
    (assoc-in db [:modal/bank-modal] value)))

(rf/reg-event-db
  ::hide-bank-modal
  (fn [db _]
    (assoc-in db [:modal/bank-modal] nil)))

