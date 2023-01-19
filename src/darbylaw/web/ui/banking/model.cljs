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

(defn asset-label [type banking-id]
  (get-in (institution-list-by-id type) [banking-id :common-name]))



(rf/reg-sub ::building-societies
  (fn [db]
    (:buildsoc-accounts (:current-case db))))

(rf/reg-sub ::banks
  (fn [db]
    (:bank-accounts (:current-case db))))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/banking db)))

(rf/reg-sub ::current-banking-type
  :<- [::dialog]
  (fn [dialog]
    (:type dialog)))

(rf/reg-sub ::dialog-open
  :<- [::dialog]
  (fn [dialog]
    (:open dialog)))

(rf/reg-sub ::current-banking-id
  :<- [::dialog]
  (fn [dialog]
    (:id dialog)))

(rf/reg-sub ::current-buildsoc-data
  :<- [::current-banking-id]
  :<- [::building-societies]
  (fn [[buildsoc-id all-buildsocs]]
    (first (filter #(= (:buildsoc-id %) buildsoc-id) all-buildsocs))))

(rf/reg-sub ::current-bank-data
  :<- [::current-banking-id]
  :<- [::banks]
  (fn [[bank-id all-banks]]
    (first (filter #(= (:bank-id %) bank-id) all-banks))))

(rf/reg-sub ::current-asset-data
  :<- [::current-banking-type]
  :<- [::current-bank-data]
  :<- [::current-buildsoc-data]
  (fn [[type bank-data buildsoc-data]]
    (case type
      :bank bank-data
      :buildsoc buildsoc-data)))

(rf/reg-sub ::current-notification-letter
  :<- [::current-asset-data]
  (fn [asset-data]
    (:notification-letter asset-data)))

(rf/reg-sub ::current-valuation-letter
  :<- [::current-asset-data]
  (fn [asset-data]
    (:valuation-letter asset-data)))

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

(rf/reg-sub ::current-process-stage
  :<- [::dialog]
  :<- [::current-asset-data]
  (fn [[dialog asset-data]]
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
(def letter-loading? (r/atom false))
(ui/reg-fx+event ::reset-letter-loading
  (fn [_]
    (reset! letter-loading? false)))

(rf/reg-event-fx ::generate-notification-failure
  (fn [{:keys [db]} [_ case-id banking-id response]]
    {:db (assoc-in db [:current-case :failure banking-id] response)
     :fx [[:dispatch [::reset-letter-loading]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::generate-notification-success
  (fn [{:keys [db]} [_ case-id banking-id response]]
    {:db (assoc-in db [:current-case :success banking-id] response)
     :fx [[:dispatch [::reset-letter-loading]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::generate-notification
  (fn [{:keys [db]} [_ case-id values]]
    (let [type (if (some? (:bank-id values))
                 :bank
                 :buildsoc)
          banking-id (case type
                       :bank (:bank-id values)
                       :buildsoc (:buildsoc-id values))]
      {:http-xhrio
       (ui/build-http
         {:method :post
          :uri (str "/api/case/" case-id "/" (name type) "/" (name banking-id) "/generate-notification-letter")
          :on-success [::generate-notification-success case-id banking-id]
          :on-failure [::generate-notification-failure case-id banking-id]})})))

(rf/reg-event-fx ::approve-notification-letter-success
  (fn [{:keys [db]} [_ case-id]]
    {:fx [[:dispatch [::hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::approve-notification-letter
  (fn [{:keys [db]} [_ type case-id banking-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name banking-id) "/approve-notification-letter/" letter-id)
        :on-success [::approve-notification-letter-success case-id banking-id]})}))

(rf/reg-event-fx ::review-notification-letter--success
  (fn [{:keys [db]} [_ case-id banking-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::review-notification-letter
  (fn [{:keys [db]} [_ type send-action case-id banking-id letter-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name banking-id)
               "/notification-letter/" letter-id "/review")
        :params {:send-action send-action}
        :on-success [::review-notification-letter--success case-id banking-id]})}))

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
  (fn [_ [_ type case-id banking-id file suffix]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name type) "/" (name banking-id) suffix)
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
