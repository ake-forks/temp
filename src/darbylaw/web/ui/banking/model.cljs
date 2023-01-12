(ns darbylaw.web.ui.banking.model
  (:require
    [darbylaw.api.buildsoc-list :as buildsoc-list]
    [darbylaw.api.bank-list :as bank-list]
    [re-frame.core :as rf]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [reagent-mui.components :as mui]
    [reagent.core :as r]))


(defn institution-list [type]
  (if (some? type)
    (case type
      "buildsoc" buildsoc-list/buildsoc-list
      "bank" bank-list/bank-list)))
(defn institution-list-by-id [type]
  (if (some? type)
    (case type
      "buildsoc" (into {} (map (juxt :id identity) buildsoc-list/buildsoc-list))
      "bank" (into {} (map (juxt :id identity) bank-list/bank-list)))))


(defn all-institution-ids [type]
  (case type
    "buildsoc" (map :id buildsoc-list/buildsoc-list)
    "bank" (map :id bank-list/bank-list)))

(defn asset-label [type asset-id]
  (get-in (institution-list-by-id type) [asset-id :common-name]))



(rf/reg-sub ::building-societies
  (fn [db]
    (:buildsoc-accounts (:current-case db))))

(rf/reg-sub ::get-dialog
  (fn [db]
    (try
      (:dialog/banking db))))

(rf/reg-sub ::get-type
  :<- [::get-dialog]
  (fn [dialog]
    (:type dialog)))

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

(defn valuation-letter-present? []
  (contains? @(rf/subscribe [::current-buildsoc-data]) :valuation-letter))


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
    (assoc-in db [:dialog/banking]
      {:open true
       :id id})))

(rf/reg-event-db
  ::show-add-dialog
  (fn [db [_ type]]
    (assoc-in db [:dialog/banking]
      {:open true
       :id nil
       :stage :add
       :type type})))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/banking] nil)))
