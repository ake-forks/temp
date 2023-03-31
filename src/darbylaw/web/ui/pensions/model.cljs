(ns darbylaw.web.ui.pensions.model
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :refer (<<) :as ui]))

;dashboard and dialog controls
(def anchor (r/atom nil))

(def edit-mode (r/atom false))

(rf/reg-event-db
  ::show-dialog
  (fn [db [_ id pension-type dialog-type]]
    (reset! anchor nil)
    (assoc-in db [:dialog/pensions]
      {:open true
       :id (str id)
       :pension-type pension-type
       :dialog-type dialog-type})))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/pensions] {:open false})))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/pensions db)))

;data
(def provider-options
  [{:id :aviva
    :common-name "Aviva"}
   {:id :nest
    :common-name "Nest"}
   {:id :standard-life
    :common-name "Standard Life"}
   {:id :hargreaves-lansdown
    :common-name "Hargreaves Lansdown"}
   {:id :other
    :common-name "Provider Not Listed"}])
(rf/reg-sub ::providers
  (fn [_]
    provider-options))

(rf/reg-sub ::all-provider-ids
  :<- [::providers]
  (fn [providers]
    (map :id providers)))

(defn id->label-fn [coll]
  (let [label-by-id (into {} (map (juxt :id :common-name) coll))]
    (fn [id]
      (get label-by-id (keyword id)))))

(rf/reg-sub ::provider-id->label
  :<- [::providers]
  (fn [providers]
    (id->label-fn providers)))

(rf/reg-sub ::pensions
  :<- [::case-model/current-case]
  (fn [case]
    (:pensions case)))

(defn get-pension [provider]
  (if (= :state provider)
    (first (filter #(= :state (:pension-type %)) (<< ::pensions)))
    (first (filter #(= provider (:provider %)) (<< ::pensions)))))

(defn get-label [provider]
  (if (= :state provider)
    "state pension"
    (-> (into {} (map (juxt :id :common-name) (<< ::providers)))
      (get provider))))

;edit
(rf/reg-event-fx ::edit-success
  (fn [_ [_  case-id]]
    (reset! edit-mode false)
    {:fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::edit-failure
  (fn [_ [_ response]]
    (reset! edit-mode false)
    {:dispatch [::ui/notify-user-http-error "edit failed" response]}))

(rf/reg-event-fx ::edit-pension
  (fn [_ [_ pension-type case-id pension-id {:keys [values]}]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri
        (case pension-type
          :private (str "/api/case/" case-id "/pension/edit-private/" pension-id)
          :state (str "/api/case/" case-id "/pension/edit-state/" pension-id))
        :params (case pension-type
                  :private (select-keys values [:reference :provider])
                  :state (select-keys values [:reference :start-date]))
        :on-success [::edit-success case-id]
        :on-failure [::edit-failure]})}))