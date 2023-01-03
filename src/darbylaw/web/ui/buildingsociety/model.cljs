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

(defn build-soc-data [buildsoc-id]
  (let [all-data @(rf/subscribe [::building-societies])]
    (first (filter #(= (:buildsoc-id %) buildsoc-id) all-data))))


;TODO which is better? Not creating new subs but deriving from rf/reg-sub ::building-societies:
(defn notification-letter-id [buildsoc-id]
  (get-in (build-soc-data buildsoc-id) [:notification-letter :id]))

;TODO or using a nested reg-sub (example from bank-model):
#_(rf/reg-sub ::notification-letter-id
    :<- [::current-bank-data]
    (fn [bank-data]
      (get-in bank-data [:notification-letter :id])))


(defn get-process-stage [id]
  (let [all-buildsocs @(rf/subscribe [::building-societies])
        dialog @(rf/subscribe [::get-dialog])]
    (if (= (:stage dialog) :add)
      :add
      (if (some? (filter #(= :buildsoc-id %) all-buildsocs))
        (if (= (:notification-status (build-soc-data id)) :started)
          :notify
          (if (contains? (:notification-letter (build-soc-data id)) :approved)

            :valuation
            :edit))))))


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
  (fn [{:keys [db]} [_ case-id buildsoc-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

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
       :id id
       :stage :edit #_(get-process-stage id)})))

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
