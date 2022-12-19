(ns darbylaw.web.ui.buildingsociety.model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]))

(rf/reg-sub ::building-societies
  (fn [db]
    (:building-societies (:current-case db))))

(rf/reg-sub ::get-dialog
  (fn [db]
    (try
      (:dialog/building-society db))))


(defn build-soc-data [id]
  (let [all-data @(rf/subscribe [::building-societies])]
    (get all-data id)))

(defn get-process-stage [id]
  (let [data (build-soc-data id)]
    (assert (some? data) "building society data not found")
    (case (:notification-status data)
      :started :approve-letter
      (or :notification-letter-sent :values-uploaded) :confirm-values
      :values-confirmed :completed
      :else
      :edit-accounts)))

(rf/reg-event-db
  ::show-process-dialog
  (fn [db [_ id]]
    (assoc-in db [:dialog/building-society]
      {:open true
       :id id
       :stage (get-process-stage id)})))

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
