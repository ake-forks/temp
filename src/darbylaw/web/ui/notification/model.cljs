(ns darbylaw.web.ui.notification.model
  (:require [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bills.model :as bills-model]))

(defn set-current-notification [db notification]
  (assoc db :notification notification))

(rf/reg-sub ::notification
  #(:notification %))

(rf/reg-sub ::notification-process
  :<- [::case-model/current-case]
  :<- [::notification]
  (fn [[case-data notification]]
    (->> (:notification-process case-data)
      (filter #(= notification (select-keys % [:asset-type
                                               :utility-company
                                               :property])))
      first)))

(rf/reg-sub ::notification-ongoing?
  :<- [::notification-process]
  (fn [process]
    (:ready-to-start process)))

(rf/reg-sub ::notification-type
  :<- [::notification]
  #(:notification-type %))

(rf/reg-sub ::utility-company-label
  :<- [::bills-model/company-id->label]
  :<- [::notification]
  (fn [[id->label context]]
    (id->label (:utility-company context))))

(rf/reg-event-fx ::start-notification-process-success
  (fn [{:keys [db]} [_ case-id _response]]
    {:db (-> db
           (assoc ::dialog-open? false))
     ; Should we wait until case is loaded to close the dialog?
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::start-notification-process-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::start-notification-process
  (fn [_ [_ case-id context]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/start-notification-process")
        :params (merge {:asset-type :utility-bill}
                  context)
        :on-success [::start-notification-process-success case-id]
        :on-failure [::start-notification-process-failure]})}))

;dialog events and subs - moved here because the accounts component needs them which would cause a circular dependency
(rf/reg-event-db ::open
  (fn [db [_ notification]]
    (-> db
      (set-current-notification notification)
      (assoc ::context {:dialog-open? true}))))

(rf/reg-sub ::context #(::context %))

(rf/reg-sub ::dialog-open?
  :<- [::context]
  #(:dialog-open? %))
(rf/reg-event-db ::close-dialog
  (fn [db _]
    (assoc-in db [::context :dialog-open?] false)))

(rf/reg-event-db ::set-data-completed
  (fn [db [_ completed?]]
    (assoc-in db [::context :data-completed?] completed?)))

(rf/reg-sub ::data-completed?
  :<- [::context]
  #(get % :data-completed? false))
