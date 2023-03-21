(ns darbylaw.web.ui.vehicle.model
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]))


;; >> Data

(rf/reg-sub ::vehicles
  :<- [::case-model/current-case]
  (fn [current-case _]
    (:vehicles current-case)))
