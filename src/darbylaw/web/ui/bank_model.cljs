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

(rf/reg-sub ::current-bank-data
  :<- [::case-model/current-case]
  :<- [::bank-id]
  (fn [[current-case bank-id]]
    (first (->> (:bank-accounts current-case)
             (filter (fn [bank]
                       (= bank-id (:bank-id bank))))))))

(comment
  (def s (rf/subscribe [::current-bank-data])))

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

(rf/reg-sub ::notification-letter-id
  :<- [::current-bank-data]
  (fn [bank-data]
    (get-in bank-data [:notification-letter :id])))

; Bank notification starts

(rf/reg-event-fx ::generate-notification-letter--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:db (assoc-in db [:current-case :bank bank-id ::notification-process-starting?] false)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::generate-notification-letter
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:db (assoc-in db [:current-case :bank bank-id ::notification-process-starting?] true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/generate-notification-letter")
        :on-success [::generate-notification-letter--success case-id bank-id]})}))

(defn ongoing-notification-process? [db bank-id]
  (let [status (get-in db [:current-case :bank bank-id :notification-status])]
    (and (some? status)
      (not= :cancelled status))))

(rf/reg-sub ::start-notification-hidden?
  (fn [db [_ case-id bank-id]]
    (ongoing-notification-process? db bank-id)))

(rf/reg-event-fx ::approve-notification-letter--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::approve-notification-letter
  (fn [{:keys [db]} [_ case-id bank-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/approve-notification-letter/" letter-id)
        :on-success [::approve-notification-letter--success case-id bank-id]})}))

(rf/reg-event-fx ::mark-values-confirmed--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::mark-values-confirmed
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/mark-values-confirmed")
        :on-success [::mark-values-confirmed--success case-id bank-id]})}))

