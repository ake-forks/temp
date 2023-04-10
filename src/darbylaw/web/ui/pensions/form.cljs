(ns darbylaw.web.ui.pensions.form
  (:require
    [clojure.string :as string]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.util.form :as form-util]
    [reagent-mui.components :as mui]
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
    :label "reference/policy number if known"}])

(defn tell-us-once-field [fork-args]
  [form-util/text-field fork-args
   {:name :tell-us-once
    :label "tell us once reference"}])

(defn start-date-field [fork-args]
  [form-util/text-field fork-args
   {:name :start-date
    :label "pension start date"}])

(defn toggle-negative [s]
  (if (string/starts-with? s "-")
    (subs s 1)
    (str "-" s)))

(defn value-field [{:keys [state values] :as fork-args}]
  [mui/stack {:direction :row :spacing 0.5}
   [form-util/text-field fork-args {:name :valuation
                                    :label "account value"
                                    :required true
                                    :style {:width "50%"}
                                    :InputProps
                                    {:start-adornment
                                     (r/as-element
                                       [mui/input-adornment {:position :start} "£"])}}]
   [mui/stack {:direction :row
               :align-items :center
               :spacing 0.5
               :style {:width "50%"}}
    [mui/typography {:variant :body2} "owed to estate"]
    [mui/switch {:checked (string/starts-with? (or (:valuation values) " ") "-")
                 :on-click #(swap! state update-in [:values :valuation] toggle-negative)}]
    [mui/typography {:variant :body2} "in debt"]]])

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
        :initial-values (merge initial-values (select-keys deceased [:ni-number :tell-us-once]))}
       (fn [fork-args]
         [layout (ui/mui-fork-args fork-args)])]
      (finally
        (reset! form-state nil)))))
