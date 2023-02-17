(ns darbylaw.web.ui.bills.model
  (:require
    [darbylaw.api.util.data :as data-util]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [darbylaw.api.bill.data :as bills-data]
    [darbylaw.web.ui.case-model :as case-model]
    [medley.core :as medley]
    [reagent.core :as r]))

(rf/reg-sub ::companies
  (fn [_]
    bills-data/companies))

(rf/reg-sub ::all-company-ids
  :<- [::companies]
  (fn [companies]
    (map :id companies)))

(defn id->label-fn [coll]
  (let [label-by-id (into {} (map (juxt :id :common-name) coll))]
    (fn [id]
      (or (get label-by-id (keyword id))
        (name id)))))

(rf/reg-sub ::company-id->label
  :<- [::companies]
  (fn [companies]
    (id->label-fn companies)))

(rf/reg-sub ::councils
  (fn [_]
    bills-data/councils))

(rf/reg-sub ::all-council-ids
  :<- [::councils]
  (fn [councils]
    (map :id councils)))

(rf/reg-sub ::council-id->label
  :<- [::councils]
  (fn [councils]
    (id->label-fn councils)))

(rf/reg-sub ::bill-types
  (fn [_]
    (for [[name {:keys [label]}] bills-data/bill-types]
      {:name name
       :label label})))

(rf/reg-sub ::current-bills
  :<- [::case-model/current-case]
  #(:bills %))

(rf/reg-sub ::used-billing-addresses
  :<- [::current-bills]
  (fn [bills]
    (->> bills
      (map :address)
      (filter string?)
      (distinct))))

(rf/reg-sub ::current-properties
  :<- [::case-model/current-case]
  (fn [case-data]
    (:properties case-data)))

(rf/reg-sub ::current-properties-by-id
  :<- [::current-properties]
  (fn [properties]
    (medley/index-by :id properties)))

(def bills-dashboard-menu (r/atom nil))
(rf/reg-sub ::bills-dialog
  (fn [db]
    (:dialog/bills db)))

(defn values-to-submit [{:keys [values] :as _fork-params}]
  (cond-> values
    (:supplier-new values)
    (dissoc :supplier)

    (= :new-property (:property values))
    (assoc :property (:address-new values))

    :always
    (dissoc :address-new)

    (string? (:property values))
    (update :property data-util/sanitize-empty-space)))

(rf/reg-event-db
  ::show-bills-dialog
  (fn [db [_ service supplier-id]]
    (reset! bills-dashboard-menu nil)
    (assoc-in db [:dialog/bills]
      {:service service
       :id supplier-id})))

(rf/reg-event-db ::save-temp-data
  (fn [db [_ fork-params]]
    (assoc-in db [:bills/temp-data]
      (:values fork-params))))

(rf/reg-event-db ::clear-temp-data
  (fn [db]
    (assoc-in db [:bills/temp-data] nil)))

(rf/reg-sub ::get-temp-data
  (fn [db]
    (:bills/temp-data db)))

(rf/reg-event-fx ::close-clear-dialog
  (fn []
    {:fx [[:dispatch [::clear-temp-data]]
          [:dispatch [::show-bills-dialog nil]]]}))

(rf/reg-sub ::form-submitting?
  (fn [db]
    (::form-submitting? db)))

(defn set-submitting [db fork-params submitting?]
  (-> db
    (fork/set-submitting (:path fork-params) submitting?)
    (assoc ::form-submitting? submitting?)))
