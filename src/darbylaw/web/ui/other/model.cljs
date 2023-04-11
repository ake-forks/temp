(ns darbylaw.web.ui.other.model
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.api.other.data :as data]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form]
    [darbylaw.web.util.dayjs :as dayjs]
    [medley.core :as medley]))


;; >> Dialog

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::dialog-open? true
                 ::dialog-context dialog-context})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (::dialog-open? db)))

(rf/reg-sub ::dialog-context
  (fn [db]
    (::dialog-context db)))

(rf/reg-sub ::submitting?
  (fn [db]
    (::submitting? db)))


;; >> Data

(rf/reg-sub ::assets
  :<- [::case-model/current-case]
  (fn [current-case _]
    (:other-assets current-case)))

(rf/reg-sub ::assets-by-id
  :<- [::assets]
  (fn [assets _]
    (medley/index-by :asset-id assets)))

(rf/reg-sub ::asset
  :<- [::assets-by-id]
  (fn [assets-by-id [_ asset-id]]
    (get assets-by-id asset-id)))

(rf/reg-sub ::asset-form-details
  :<- [::assets-by-id]
  (fn [assets-by-id [_ asset-id]]
    (-> (get assets-by-id asset-id)
        (select-keys data/props)
        (update :paid-at dayjs/maybe-read))))



;; >> Generic Submit Effects

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::submitting? false)
     :fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ message error-result]]
    {:db (assoc db ::submitting? false)
     ::ui/notify-user-http-error {:message message
                                  :result error-result}}))



;; >> Upsert Submit Effects

(rf/reg-event-fx ::upsert-submit-success
  (fn [{:keys [db]} [_ {:keys [reset values]} case-id {asset-id :id}]]
    ;; NOTE: Prevents form-files from being re-uploaded
    (reset {:values (select-keys values data/props)})
    {:db (assoc db ::submitting? false)
     :fx [[:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::set-dialog-open asset-id]]]}))

(rf/reg-event-fx ::upsert-asset
  (fn [{:keys [db]} [_ {:keys [case-id asset-id]} {:keys [values] :as fork-args}]]
    {:db (assoc db ::submitting? true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/other"
                  (when asset-id (str "/" asset-id)))
        :body (form/->FormData values)
        :on-success [::upsert-submit-success fork-args case-id]
        :on-failure [::submit-failure (if-not asset-id
                                        "Error adding other asset"
                                        "Error updating other asset")]})}))



;; >> Upload Submit Effects

(rf/reg-event-fx ::upload-document
  (fn [_ [_ case-id asset-id document]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id
                  "/other/" asset-id
                  "/document")
        :body (form/->FormData {:-file-1 document})
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error uploading other asset document"]})}))



;; >> Delete Submit Effects

(rf/reg-event-fx ::delete-document
  (fn [_ [_ case-id asset-id document-id]]
    {:http-xhrio
     (ui/build-http
       {:method :delete
        :uri (str "/api/case/" case-id
                  "/other/" asset-id
                  "/document/" document-id)
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error deleting other asset document"]})}))
