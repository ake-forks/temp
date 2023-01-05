(ns darbylaw.web.ui.buildingsociety.model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.case-model :as case-model]
            [reagent-mui.components :as mui]
            [reagent.core :as r]))

(def buildsoc-options
  [{:id :bath-building-society
    :common-name "Bath Building Society"}
   {:id :cambridge-building-society
    :common-name "Cambridge Building Society"}
   {:id :darlington-building-society
    :common-name "Darlington Building Society"}
   {:id :harpenden-building-society
    :common-name "Harpenden Building Society"}])

(def buildsoc-accounts
  [{:buildsoc-id :bath-building-society
    :common-name "Bath Building Society"
    :accounts [{:roll-number 123 :estimated-value 100}
               {:roll-number 567 :estimated-value 250}]}
   {:buildsoc-id :cambridge-building-society
    :common-name "Cambridge Building Society"
    :accounts [{:roll-number 987 :estimated-value 400.50}
               {:roll-number 432 :estimated-value 105}]}])

(rf/reg-sub ::building-societies
  (fn [db]
    (:buildsoc-accounts (:current-case db))))

(rf/reg-sub ::get-dialog
  (fn [db]
    (try
      (:dialog/building-society db))))

(rf/reg-sub ::current-buildsoc-id
  :<- [::get-dialog]
  (fn [dialog]
    (:id dialog)))

(rf/reg-sub ::current-buildsoc-data
  :<- [::current-buildsoc-id]
  :<- [::building-societies]
  (fn [[buildsoc-id all-buildsocs]]
    (first (filter #(= (:buildsoc-id %) buildsoc-id) all-buildsocs))))

(rf/reg-sub ::notification-letter-id
  :<- [::current-buildsoc-data]
  (fn [buildsoc-data]
    (get-in buildsoc-data [:notification-letter :id])))


(defn get-process-stage []
  (let [dialog @(rf/subscribe [::get-dialog])
        buildsoc-data @(rf/subscribe [::current-buildsoc-data])]
    (if (= (:stage dialog) :add)
      :add
      ;if it contains a buildsoc-id
      (if (contains? buildsoc-data :notification-letter)
        (if (contains? (:notification-letter buildsoc-data) :approved)
          (if (contains? (first (:accounts buildsoc-data)) :confirmed-value)
            :complete
            :valuation)
          :notify)
        :edit))))

; edit =
; notify = notification letter not approved
; valuation = notification letter approved
; completed = :accounts contains :confirmed-value


;generating and approving notification letters
(rf/reg-event-fx ::generate-notification-failure
  (fn [{:keys [db]} [_ case-id buildsoc-id response]]
    {:db (assoc-in db [:current-case :failure buildsoc-id] response)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::generate-notification-success
  (fn [{:keys [db]} [_ case-id buildsoc-id response]]
    {:db (assoc-in db [:current-case :success buildsoc-id] response)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::generate-notification
  (fn [{:keys [db]} [_ case-id buildsoc-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/generate-notification-letter")
        :on-success [::generate-notification-success case-id buildsoc-id]
        :on-failure [::generate-notification-failure case-id buildsoc-id]})}))

(rf/reg-event-fx ::approve-notification-letter-success
  (fn [{:keys [db]} [_ case-id]]
    {:fx [[:dispatch [::hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::approve-notification-letter
  (fn [{:keys [db]} [_ case-id buildsoc-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/approve-notification-letter/" letter-id)
        :on-success [::approve-notification-letter-success case-id buildsoc-id]})}))


;uploading files
(def file-uploading? (r/atom false))
(def upload-error (r/atom false))

(defn upload-error-snackbar [message]
  [mui/snackbar {:open @upload-error
                 :autoHideDuration 5000
                 :on-close #(reset! upload-error false)}
   [mui/alert {:severity "error" :on-close #(reset! upload-error false)}
    (str "There was a problem uploading this file. " message)]])
(rf/reg-event-fx ::load-case-success
  (fn [_ _]
    (reset! file-uploading? false)))
(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    {:dispatch [::case-model/load-case! case-id
                {:on-success [::load-case-success]
                 :on-failure [::load-case-failure]}]}))

(rf/reg-event-fx ::upload-failure
  (fn [_ _]
    (reset! file-uploading? false)
    (reset! upload-error true)))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ case-id buildsoc-id file suffix]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) suffix)
        :body (doto (js/FormData.)
                (.append "file" file))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

;show/hide dialogs
(rf/reg-event-db
  ::show-process-dialog
  (fn [db [_ id]]
    (assoc-in db [:dialog/building-society]
      {:open true
       :id id})))

(rf/reg-event-db
  ::show-add-dialog
  (fn [db]
    (assoc-in db [:dialog/building-society]
      {:open true
       :id nil
       :stage :add})))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/building-society] nil)))
