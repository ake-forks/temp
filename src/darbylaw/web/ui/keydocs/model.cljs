(ns darbylaw.web.ui.keydocs.model
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]))


(rf/reg-event-db
  ::show-dialog
  (fn [db]
    (assoc-in db [:dialog/key-docs]
      true)))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/key-docs]
      false)))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/key-docs db)))

(def file-uploading? (r/atom false))

