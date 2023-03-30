(ns darbylaw.web.ui.pensions.form
  (:require
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.util.form :as form-util]
    [reagent.core :as r]
    [fork.re-frame :as fork]
    [darbylaw.web.ui :as ui :refer (<<)]
    [darbylaw.web.ui.case-model :as case-model]))

(defn company-select [{:keys [values] :as fork-args}]
  [form-util/autocomplete-field fork-args
   {:name :provider
    :full-width true
    :label "provider"
    :value (:provider values)
    :options (<< ::model/all-provider-ids)
    :getOptionLabel (<< ::model/provider-id->label)
    :inner-config {:required true}}])

(defn ni-field [fork-args]
    [form-util/text-field fork-args
     {:name :ni-number
      :label "national insurance number"
      :required true}])

(defn ref-field [fork-args]
  [form-util/text-field fork-args
   {:name :reference
    :label "reference number"}])



(defonce form-state (r/atom nil))
(defn form [{:keys [layout submit-fn initial-values]}]
  (let [deceased (:deceased (<< ::case-model/current-case))]
    (r/with-let []
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit submit-fn
        :keywordize-keys true
        :prevent-default? true
        :initial-values
        (if (:ni-number deceased)
          (merge initial-values {:ni-number (:ni-number deceased)})
          initial-values)}
       (fn [fork-args]
         [layout (ui/mui-fork-args fork-args)])]
      (finally
        (reset! form-state nil)))))
