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
      :buildsoc buildsoc-list/buildsoc-list
      :bank bank-list/bank-list)))

(defn institution-list-by-id [type]
  (if (some? type)
    (case type
      :buildsoc (into {} (map (juxt :id identity) buildsoc-list/buildsoc-list))
      :bank (into {} (map (juxt :id identity) bank-list/bank-list)))))

(defn all-institution-ids [type]
  (case type
    :buildsoc (map :id buildsoc-list/buildsoc-list)
    :bank (map :id bank-list/bank-list)))

(defn asset-label [type asset-id]
  (get-in (institution-list-by-id type) [asset-id :common-name]))



(rf/reg-sub ::building-societies
  (fn [db]
    (:buildsoc-accounts (:current-case db))))

(rf/reg-sub ::banks
  (fn [db]
    (:bank-accounts (:current-case db))))

(rf/reg-sub ::get-dialog
  (fn [db]
    (try
      (:dialog/banking db))))

(rf/reg-sub ::get-type
  :<- [::get-dialog]
  (fn [dialog]
    (:type dialog)))

(rf/reg-sub ::banking-type
  :<- [::get-type]
  (fn [type-str]
    (keyword type-str)))

(rf/reg-sub ::current-asset-id
  :<- [::get-dialog]
  (fn [dialog]
    (:id dialog)))

(rf/reg-sub ::current-buildsoc-data
  :<- [::current-asset-id]
  :<- [::building-societies]
  (fn [[buildsoc-id all-buildsocs]]
    (first (filter #(= (:buildsoc-id %) buildsoc-id) all-buildsocs))))

(rf/reg-sub ::current-bank-data
  :<- [::current-asset-id]
  :<- [::banks]
  (fn [[bank-id all-banks]]
    (first (filter #(= (:bank-id %) bank-id) all-banks))))

(defn get-asset-data [type]
  (if (some? type)
    (case type
      :bank @(rf/subscribe [::current-bank-data])
      :buildsoc @(rf/subscribe [::current-buildsoc-data]))))

(rf/reg-sub ::asset-data
  :<- [::banking-type]
  :<- [::current-bank-data]
  :<- [::current-buildsoc-data]
  (fn [[type bank-data buildsoc-data]]
    (case type
      :bank bank-data
      :buildsoc buildsoc-data)))

(rf/reg-sub ::notification-letter-id
  :<- [::asset-data]
  (fn [asset-data]
    (get-in asset-data [:notification-letter :id])))

(defn get-letter-id [type]
  (let [asset-data (get-asset-data type)]
    (get-in asset-data [:notification-letter :id])))

(rf/reg-sub ::author
  :<- [::asset-data]
  (fn [asset-data]
    (get-in asset-data [:notification-letter :author])))

(defn valuation-letter-present? [type]
  (contains? (get-asset-data type) :valuation-letter))


(defn get-asset-stage
  [asset-data]
  (cond
    (not (contains? asset-data :notification-letter))
    :edit
    
    (not (some? (get-in asset-data [:notification-letter :review-timestamp])))
    :notify

    (not (every? #(contains? % :confirmed-value)
                 (:accounts asset-data)))
    :valuation

    :else
    :complete))

(defn get-process-stage []
  (let [dialog @(rf/subscribe [::get-dialog])
        asset-data (get-asset-data (:type dialog))]
    (if (= (:stage dialog) :add)
      :add
      (get-asset-stage asset-data))))

(defn remove-joint [data]
  (mapv (fn [acc]
          (if (false? (:joint-check acc))
            (apply dissoc acc [:joint-check :joint-info])
            acc))
    (:accounts data)))

(defn bank-transform-on-submit [data]
  (merge {:bank-id (keyword (:bank-id data))
          :accounts (remove-joint data)}))

(defn buildsoc-transform-on-submit [values]
  (if (= true (:accounts-unknown values))
    {:buildsoc-id (keyword (:buildsoc-id values)) :accounts [] :accounts-unknown true}
    {:buildsoc-id (keyword (:buildsoc-id values)) :accounts (:accounts values) :accounts-unknown false}))

; edit =
; notify = notification letter not approved
; valuation = notification letter approved
; completed = :accounts contains :confirmed-value


;generating and approving notification letters
(rf/reg-event-fx ::generate-notification-failure
  (fn [{:keys [db]} [_ case-id asset-id response]]
    {:db (assoc-in db [:current-case :failure asset-id] response)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::generate-notification-success
  (fn [{:keys [db]} [_ case-id asset-id response]]
    {:db (assoc-in db [:current-case :success asset-id] response)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::generate-notification
  (fn [{:keys [db]} [_ case-id values]]
    (let [type (if (some? (:bank-id values))
                 :bank
                 :buildsoc)
          asset-id (case type
                     :bank (:bank-id values)
                     :buildsoc (:buildsoc-id values))]
      {:http-xhrio
       (ui/build-http
         {:method :post
          :uri (str "/api/case/" case-id "/" (name type) "/" (name asset-id) "/generate-notification-letter")
          :on-success [::generate-notification-success case-id asset-id]
          :on-failure [::generate-notification-failure case-id asset-id]})})))

(rf/reg-event-fx ::approve-notification-letter-success
  (fn [{:keys [db]} [_ case-id]]
    {:fx [[:dispatch [::hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::approve-notification-letter
  (fn [{:keys [db]} [_ type case-id asset-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name asset-id) "/approve-notification-letter/" letter-id)
        :on-success [::approve-notification-letter-success case-id asset-id]})}))

(rf/reg-event-fx ::review-notification-letter--success
  (fn [{:keys [db]} [_ case-id asset-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::review-notification-letter
  (fn [{:keys [db]} [_ type send-action case-id asset-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name asset-id)
               "/notification-letter/" letter-id "/review")
        :params {:send-action send-action}
        :on-success [::review-notification-letter--success case-id asset-id]})}))

;uploading files
(def file-uploading? (r/atom false))
(def upload-error (r/atom false))

(defn upload-error-snackbar [message]
  [mui/snackbar {:open @upload-error
                 :autoHideDuration 5000
                 :on-close #(reset! upload-error false)}
   [mui/alert {:severity "error" :on-close #(reset! upload-error false)}
    (str "There was a problem uploading this file. " message)]])

(ui/reg-fx+event ::reset-file-uploading
  (fn [_]
    (reset! file-uploading? false)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    {:dispatch [::case-model/load-case! case-id
                {:on-success [::reset-file-uploading]}]}))

(ui/reg-fx+event ::upload-failure
  (fn [_]
    (reset! file-uploading? false)
    (reset! upload-error true)))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ type case-id asset-id file suffix]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name asset-id) suffix)
        :body (doto (js/FormData.)
                (.append "file" file)
                (.append "filename" (.-name file)))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

;show/hide dialogs
(rf/reg-event-db
  ::show-process-dialog
  (fn [db [_ type id]]
    (assoc-in db [:dialog/banking]
      {:open true
       :id id
       :type type})))

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
    (assoc-in db [:dialog/banking :open] nil)))
