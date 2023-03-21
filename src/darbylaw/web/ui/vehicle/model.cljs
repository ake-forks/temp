(ns darbylaw.web.ui.vehicle.model
  (:require
    [fork.core :as fork]
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
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



;; >> Submit Effects

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ path case-id {vehicle-id :id}]]
    {:db (-> db 
             (fork/set-submitting path false)
             (assoc ::submitting? false))
     :fx [[:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::set-dialog-open vehicle-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ path message error-result]]
    {:db (-> db
             (fork/set-submitting path false)
             (assoc ::submitting? false))
     ::ui/notify-user-http-error {:message message
                                  :result error-result}}))

(rf/reg-event-fx ::upsert-vehicle
  (fn [{:keys [db]} [_ {:keys [case-id vehicle-id]} {:keys [path values]}]]
    (println "upsert-vehicle" vehicle-id)
    {:db (-> db
             (fork/set-submitting path true)
             (assoc ::submitting? true))
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/vehicle"
                  (when vehicle-id (str "/" vehicle-id)))
        :body (form/->FormData values)
        :on-success [::submit-success path case-id]
        :on-failure [::submit-failure path (if-not vehicle-id
                                             "Error adding vehicle"
                                             "Error updating vehicle")]})}))
