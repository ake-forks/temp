(ns darbylaw.web.ui.properties.model
  (:require
    [medley.core :as medley]
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :refer (<<)]))

(defn get-property [id]
  (let [all-props (<< ::case-model/properties)]
    (get (medley/index-by :id all-props) (uuid id))))

(rf/reg-event-db
  ::show-dialog
  (fn [db [_ id dialog-type]]
    (assoc-in db [:dialog/property]
      {:open true
       :id (str id)
       :dialog-type dialog-type})))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/property] {:open false})))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/property db)))

