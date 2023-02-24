(ns darbylaw.web.ui.bills.model
  (:require
    [darbylaw.api.util.data :as data-util]
    [darbylaw.web.ui :as ui :refer [<<]]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [darbylaw.api.bill.data :as bills-data]
    [darbylaw.web.ui.case-model :as case-model]
    [medley.core :as medley]
    [reagent.core :as r]
    [clojure.string :as string]))

;TODO - go through and remove/amalgamate some of these subscriptions
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

(rf/reg-sub ::current-council-tax
  :<- [::case-model/current-case]
  (fn [case-data]
    (:council-tax case-data)))

(rf/reg-sub ::current-properties-by-id
  :<- [::current-properties]
  (fn [properties]
    (medley/index-by :id properties)))

(defn address-by-property-id [id]
  (let [properties @(rf/subscribe [::current-properties-by-id])]
    (:address (get properties id))))

(defn current-utility-data [utility-company property]
  (let [current-case (<< ::case-model/current-case)
        bills-by-property-id (group-by :property (:utility-bills current-case))]
    (filter #(= utility-company (:utility-company %))
      (get bills-by-property-id (uuid (str property))))))

(defn current-council-data [council property]
  (let [current-case (<< ::case-model/current-case)
        councils-by-property-id (group-by :property (:council-tax current-case))]
    (filter #(= council (:council %))
      (get councils-by-property-id (uuid (str property))))))

(def bills-dashboard-menu (r/atom nil))
(rf/reg-sub ::bills-dialog
  (fn [db]
    (:dialog/bills db)))

(defn create-company-id [s]
  (-> s
    (string/replace " " "-")
    (string/lower-case)
    (keyword)))

(defn values-to-submit [{:keys [values] :as _fork-params}]
  (cond-> values
    (:council values)
    (assoc :council (keyword (:council values)))

    (:utility-company values)
    (assoc :utility-company (keyword (:utility-company values)))

    ;I have assoc'ed a custom utility company with the same :utility-company key as an existing one, but to differentiate
    ;I added a new? flag (so later we can eg prompt lawyers to add the company to our existing list with an address)
    ;This is because accounting for 2 attributes when looking for the name of the supplier made the later UI a mess
    ;An alternative would be to do :new-utility-company as :utility-company in the current-case query so they are consistent
    ;in the app-db even if they're not in xtdb
    (:new-utility-company values)
    (assoc :new-utility-company? true)

    (:new-utility-company values)
    (assoc :utility-company (create-company-id (:new-utility-company values)))

    (= :new-property (:property values))
    (assoc :property (:address-new values))

    (= :deceased (:property values))
    (assoc :property @(rf/subscribe [::case-model/deceased-address]))

    :always
    (dissoc :address-new)

    (string? (:property values))
    (update :property data-util/sanitize-empty-space)))

(rf/reg-event-db
  ::show-bills-dialog
  (fn [db [_ context]]
    (reset! bills-dashboard-menu nil)
    (assoc-in db [:dialog/bills] context)))

(rf/reg-event-db ::save-temp-data
  (fn [db [_ fork-params]]
    (assoc-in db [:bills/temp-data]
      (:values fork-params))))

(rf/reg-event-db ::clear-temp-data
  (fn [db]
    (dissoc db :bills/temp-data)))

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

;uploading files
(def file-uploading? (r/atom false))
(def upload-error (r/atom false))

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
    (ui/http-error-user-message "error")))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ asset-type case-id asset-id selected-file]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" (name asset-type) "/document/" asset-id)
        :body (doto (js/FormData.)
                (.append "file" selected-file)
                (.append "filename" (.-name selected-file)))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))