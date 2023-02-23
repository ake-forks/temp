(ns darbylaw.web.ui.identity.model
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.api.smart-search.data :as ss-data]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(rf/reg-sub ::uk-aml
  :<- [::case-model/current-case]
  #(when-let [uk-aml (:uk-aml %)]
     (assoc uk-aml :final-result (ss-data/uk-aml->result uk-aml))))

(rf/reg-sub ::fraudcheck
  :<- [::case-model/current-case]
  #(when-let [fraudcheck (:fraudcheck %)]
     (assoc fraudcheck :final-result (ss-data/fraudcheck->result fraudcheck))))

(rf/reg-sub ::smartdoc
  :<- [::case-model/current-case]
  #(when-let [smartdoc (:smartdoc %)]
     (assoc smartdoc :final-result (ss-data/smartdoc->result smartdoc))))

(rf/reg-sub ::has-checks?
  :<- [::uk-aml]
  :<- [::fraudcheck]
  :<- [::smartdoc]
  (fn [[uk-aml fraudcheck smartdoc] _]
    (or (seq uk-aml) (seq fraudcheck) (seq smartdoc))))

(rf/reg-sub ::current-final-result
  :<- [::has-checks?]
  :<- [::uk-aml]
  :<- [::fraudcheck]
  :<- [::smartdoc]
  (fn [[has-checks? uk-aml fraudcheck smartdoc]]
    (if-not has-checks?
      :unknown
      (if (= :processing (:final-result smartdoc))
        :processing
        (if (= #{:pass}
               (->> [uk-aml fraudcheck smartdoc]
                    (map :final-result)
                    (into #{})))
          :pass
          :fail)))))
