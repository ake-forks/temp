(ns darbylaw.web.ui.funeral.model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case]))


;; >> Subscriptions

;; Funeral Account
(rf/reg-sub ::account
  :<- [::case/current-case]
  #(:funeral-account %))

;; Dialog Info
;; Either a keyword, or an id
(rf/reg-sub ::dialog-info
  (fn [db]
    (:dialog/funeral-dialog-info db)))

(rf/reg-sub ::expense-list
  :<- [::case/current-case]
  #(:funeral-expense %))

(rf/reg-sub ::expense-by-id
  :<- [::expense-list]
  (fn [expenses]
    (zipmap (map :expense-id expenses)
            expenses)))

(rf/reg-sub ::expense
  :<- [::expense-by-id]
  (fn [expenses [_ id]]
    (get expenses id)))



;; >> Event handlers

(rf/reg-event-db
  ::show-funeral-dialog
  (fn [db [_ value]]
    (assoc-in db [:dialog/funeral-dialog-info] value)))

(rf/reg-event-db
  ::hide-funeral-dialog
  (fn [db _]
    (assoc-in db [:dialog/funeral-dialog-info] nil)))
