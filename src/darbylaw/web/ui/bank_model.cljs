(ns darbylaw.web.ui.bank-model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [darbylaw.api.bank-list :as banks]
            [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-sub ::bank-id
  (fn [db]
    (:modal/bank-dialog db)))

(rf/reg-sub ::bank-name
  :<- [::bank-id]
  banks/bank-label)

(rf/reg-event-db
  ::show-bank-dialog
  (fn [db [_ value]]
    (assoc-in db [:modal/bank-dialog] value)))

(rf/reg-event-db
  ::hide-bank-dialog
  (fn [db _]
    (assoc-in db [:modal/bank-dialog] nil)))

(rf/reg-sub ::bank-dialog
  (fn [db _]
    (:modal/bank-dialog db)))

;temporary
(rf/reg-event-db
  ::mark-bank-complete
  (fn [db [_ bank-id]]
    (update-in db [:banks-complete] conj bank-id)))

(rf/reg-sub ::banks-complete
  (fn [db _]
    (:banks-complete db)))

; Bank notification starts

(rf/reg-event-fx ::start-notification-process--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:db (assoc-in db [:current-case :bank bank-id ::notification-process-starting?] false)
     :dispatch [::load! case-id]}))

(rf/reg-event-fx ::start-notification-process
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:db (assoc-in db [:current-case :bank bank-id ::notification-process-starting?] true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/start-notification")
        :on-success [::start-notification-process--success case-id bank-id]})}))

(defn ongoing-notification-process? [db bank-id]
  (let [status (get-in db [:current-case :bank bank-id :notification-status])]
    (and (some? status)
      (not= :cancelled status))))

(rf/reg-sub ::start-notification-hidden?
  (fn [db [_ case-id bank-id]]
    (ongoing-notification-process? db bank-id)))

(rf/reg-event-fx ::post-letter--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::post-letter
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/post-letter")
        :on-success [::post-letter--success case-id bank-id]})}))
