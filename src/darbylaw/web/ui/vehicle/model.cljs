(ns darbylaw.web.ui.vehicle.model
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.api.vehicle.data :as data]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form]
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

(rf/reg-sub ::vehicles
  :<- [::case-model/current-case]
  (fn [current-case _]
    (:vehicles current-case)))

(rf/reg-sub ::vehicles-by-id
  :<- [::vehicles]
  (fn [vehicles _]
    (medley/index-by :vehicle-id vehicles)))

(rf/reg-sub ::vehicle
  :<- [::vehicles-by-id]
  (fn [vehicles-by-id [_ vehicle-id]]
    (get vehicles-by-id vehicle-id)))

(rf/reg-sub ::vehicle-form-details
  :<- [::vehicles-by-id]
  (fn [vehicles-by-id [_ vehicle-id]]
    (-> (get vehicles-by-id vehicle-id)
        (select-keys data/props))))



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
  (fn [{:keys [db]} [_ {:keys [reset values]} case-id {vehicle-id :id}]]
    ;; NOTE: Prevents form-files from being re-uploaded
    (reset {:values (select-keys values data/props)})
    {:db (assoc db ::submitting? false)
     :fx [[:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::set-dialog-open vehicle-id]]]}))

(rf/reg-event-fx ::upsert-vehicle
  (fn [{:keys [db]} [_ {:keys [case-id vehicle-id]} {:keys [values] :as fork-args}]]
    {:db (assoc db ::submitting? true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/vehicle"
                  (when vehicle-id (str "/" vehicle-id)))
        :body (form/->FormData values)
        :on-success [::upsert-submit-success fork-args case-id]
        :on-failure [::submit-failure (if-not vehicle-id
                                        "Error adding vehicle"
                                        "Error updating vehicle")]})}))



;; >> Upload Submit Effects

(rf/reg-event-fx ::upload-document
  (fn [_ [_ case-id vehicle-id document]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id
                  "/vehicle/" vehicle-id
                  "/document")
        :body (form/->FormData {:-file-1 document})
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error uploading vehicle document"]})}))



;; >> Delete Submit Effects

(rf/reg-event-fx ::delete-document
  (fn [_ [_ case-id vehicle-id document-id]]
    {:http-xhrio
     (ui/build-http
       {:method :delete
        :uri (str "/api/case/" case-id
                  "/vehicle/" vehicle-id
                  "/document/" document-id)
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error deleting vehicle document"]})}))
