(ns darbylaw.web.ui.identity.model
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(rf/reg-sub ::current-checks
  :<- [::case-model/current-case]
  #(:checks %))

(rf/reg-sub ::current-results
  :<- [::current-checks]
  #(->> % (map :result) (into #{})))

(rf/reg-sub ::current-final-result
  :<- [::current-results]
  #(cond
     (contains? % "fail") :fail
     (contains? % "refer") :refer
     (= % #{"pass"}) :pass
     :else :unknown))
