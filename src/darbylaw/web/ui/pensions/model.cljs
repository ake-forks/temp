(ns darbylaw.web.ui.pensions.model
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]))

;dashboard and dialog controls
(def anchor (r/atom nil))

(rf/reg-event-db
  ::show-dialog
  (fn [db [_ id pension-type dialog-type]]
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
    :common-name "Hargreaves Lansdown"}])
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
