(ns darbylaw.web.ui.buildingsociety.model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]))

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


(defn build-soc-data [id]
  (let [all-data @(rf/subscribe [::building-societies])]
    (filter #(= (:buildsoc-id %) id) all-data)))

;derive stage so far without relying on :notification-status
(defn get-process-stage [id]
  (let [all-buildsocs @(rf/subscribe [::building-societies])
        dialog @(rf/subscribe [::get-dialog])]
    (if (= (:stage dialog) :add)
      :add
      (if (some? (filter #(= :buildsoc-id %) all-buildsocs))
        (if (= (first (:notification-letter (first (build-soc-data id)))) :started)
          :notify
          (if (= (first (:notification-letter (first (build-soc-data id)))) :approved)
            :valuation
            :edit))))))

;:notify
; valuation)))

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
