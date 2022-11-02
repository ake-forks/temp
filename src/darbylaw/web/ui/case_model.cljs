(ns darbylaw.web.ui.case-model
  (:require [re-frame.core :as rf]))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::nickname
  :<- [::current-case]
  #(-> % :personal-representative :forename))
