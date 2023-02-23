(ns darbylaw.web.ui.notification.model
  (:require [darbylaw.web.ui :as ui]
            [medley.core :as medley]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bills.model :as bills-model]))

(defn set-current-notification [db notification]
  (-> db
    (assoc :notification notification
           :conversation-loading :loading)
    (dissoc :open-letter
            :conversation)))

(defn get-notification [db]
  (:notification db))

(rf/reg-sub ::notification
  get-notification)

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
    {:db (-> db
           (assoc :conversation data)
           (dissoc :conversation-loading))}))

(rf/reg-event-fx ::load-conversation-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:message "Conversation could not be loaded."
                                  :result error-result}}))

(rf/reg-event-fx ::load-conversation
  (fn [{:keys [db]} [_ notification]]
    {:db (assoc db :conversation-loading :loading)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" (:case-id notification) "/conversation")
        :params notification
        :on-success [::load-conversation-success]
        :on-failure [::load-conversation-failure]})}))

(rf/reg-sub ::conversation
  #(not-empty (:conversation %)))

(rf/reg-sub ::conversation-loading
  #(:conversation-loading %))

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

(rf/reg-event-db ::open-letter
  (fn [db [_ letter-id]]
    (assoc db :open-letter letter-id)))

(rf/reg-event-db ::close-letter
  (fn [db _]
    (dissoc db :open-letter)))

(rf/reg-sub ::open-letter-id
  #(:open-letter %))

(rf/reg-sub ::letter-by-id
  :<- [::conversation]
  (fn [conversation]
    (medley/index-by :xt/id conversation)))

(rf/reg-sub ::open-letter
  :<- [::letter-by-id]
  :<- [::open-letter-id]
  (fn [[letter-by-id id]]
    (get letter-by-id id)))

(rf/reg-sub ::letter-in-preparation?
  :<- [::open-letter]
  (fn [letter]
    (not (:send-action letter))))

(rf/reg-event-fx ::send-letter-success
  (fn [{:keys [db]} _]
    {:dispatch [::load-conversation (get-notification db)]}))

(rf/reg-event-fx ::send-letter-failure
  (fn [{:keys [db]} [_ error-result]]
    {:dispatch [::load-conversation (get-notification db)]
     ::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::send-letter
  (fn [{:keys [db]} [_ {:keys [case-id letter-id fake]}]]
    {:db (update db :conversation
           #(cond-> %
              (seqable? %) (into [{:xt/id (random-uuid)
                                   :type ::creating}])))
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/notification-letter/" letter-id "/send")
        :params {:send-action (if fake :fake-send :send)}
        :on-success [::send-letter-success]
        :on-failure [::send-letter-failure]})}))

(rf/reg-fx ::replace-letter-success
  (fn [{:keys [on-completed]}]
    (on-completed)))

(rf/reg-event-fx ::replace-letter-success
  (fn [_ [_ on-completed]]
    {::replace-letter-success {:on-completed on-completed}}))

(rf/reg-event-fx ::replace-letter-failure
  (fn [_ [_ on-completed error-result]]
    {::replace-letter-success {:on-completed on-completed}
     ::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::replace-letter
  (fn [_ [_ {:keys [case-id letter-id file on-completed]}]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/notification-letter/" letter-id "/docx")
        :body (doto (js/FormData.)
                (.append "file" file)
                (.append "filename" (.-name file)))
        :format nil
        :timeout 16000
        :on-success [::replace-letter-success on-completed]
        :on-failure [::replace-letter-failure on-completed]})}))

(rf/reg-fx ::delete-letter-completed
  (fn [{:keys [on-completed]}]
    (on-completed)))

(rf/reg-event-fx ::delete-letter-success
  (fn [{:keys [db]} [_ on-completed]]
    (let [current-notification (:notification db)]
      {:db (set-current-notification db current-notification)
       :dispatch [::load-conversation current-notification]
       ::delete-letter-completed {:on-completed on-completed}})))

(rf/reg-event-fx ::delete-letter-failure
  (fn [_ [_ on-completed error-result]]
    {::delete-letter-completed {:on-completed on-completed}
     ::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::delete-letter
  (fn [_ [_ {:keys [case-id letter-id on-completed]}]]
    {:http-xhrio
     (ui/build-http
       {:method :delete
        :uri (str "/api/case/" case-id "/notification-letter/" letter-id)
        :on-success [::delete-letter-success on-completed]
        :on-failure [::delete-letter-failure on-completed]})}))

;dialog events and subs

(rf/reg-event-fx ::open
  (fn [{:keys [db]} [_ notification]]
    {:db (-> db
           (set-current-notification notification)
           (assoc ::context {:dialog-open? true}))
     :dispatch [::load-conversation notification]}))

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
