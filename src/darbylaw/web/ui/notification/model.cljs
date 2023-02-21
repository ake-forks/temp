(ns darbylaw.web.ui.notification.model
  (:require [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bills.model :as bills-model]))

(defn set-current-notification [db notification]
  (assoc db
    :notification notification
    :conversation :loading))

(rf/reg-sub ::notification
  #(:notification %))

(rf/reg-sub ::notification-process
  :<- [::case-model/current-case]
  :<- [::notification]
  (fn [[case-data current-notification]]
    (->> (:notification-process case-data)
      (filter (fn [notification]
                (let [select-relevant #(select-keys % [:notification-type
                                                       :utility-company
                                                       :property])]
                  (= (select-relevant current-notification)
                     (select-relevant notification)))))
      first)))

(rf/reg-sub ::notification-type
  :<- [::notification]
  #(:notification-type %))

(rf/reg-sub ::utility-company-label
  :<- [::bills-model/company-id->label]
  :<- [::notification]
  (fn [[id->label context]]
    (id->label (:utility-company context))))

(rf/reg-event-fx ::start-notification-success
  (fn [_ [_ case-id _response]]
    {:dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::start-notification-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::start-notification
  (fn [_ [_ notification]]
    (let [case-id (:case-id notification)]
      {:http-xhrio
       (ui/build-http
         {:method :post
          :uri (str "/api/case/" case-id "/start-notification-process")
          :params notification
          :on-success [::start-notification-success case-id]
          :on-failure [::start-notification-failure]})})))

(rf/reg-sub ::notification-ongoing?
  :<- [::notification-process]
  (fn [process]
    (:ready-to-start process)))

(rf/reg-event-fx ::load-conversation-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :conversation data)}))

(rf/reg-event-fx ::load-conversation-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:message "Conversation could not be loaded."
                                  :result error-result}}))

(rf/reg-event-fx ::load-conversation
  (fn [_ [_ notification]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" (:case-id notification) "/conversation")
        :params notification
        :on-success [::load-conversation-success]
        :on-failure [::load-conversation-failure]})}))

(rf/reg-sub ::conversation
  (fn [{:keys [conversation]}]
    (cond-> conversation
      (seqable? conversation) (not-empty))))

(rf/reg-event-fx ::generate-notification-letter-success
  (fn [_ [_ notification]]
    {:dispatch [::load-conversation notification]}))

(rf/reg-event-fx ::generate-notification-letter-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::generate-notification-letter
  (fn [{:keys [db]} [_ notification]]
    {:db (update db :conversation
           #(cond-> %
              (seqable? %) (into [{:xt/id (random-uuid)
                                   :type ::creating}])))
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" (:case-id notification) "/generate-notification-letter")
        :params notification
        :timeout 16000
        :on-success [::generate-notification-letter-success notification]
        :on-failure [::generate-notification-letter-failure]})}))

