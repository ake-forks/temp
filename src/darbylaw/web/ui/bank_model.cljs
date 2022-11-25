(ns darbylaw.web.ui.bank-model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]))

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