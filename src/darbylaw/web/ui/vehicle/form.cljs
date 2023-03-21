(ns darbylaw.web.ui.vehicle.form
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.ui.vehicle.model :as model]
            [darbylaw.web.util.form :as form]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vlad.core :as v]
            [darbylaw.web.util.vlad :as v-util]))

(defn registration-number [fork-args]
  [form/text-field fork-args
   {:name :registration-number
    :label "registration number"
    :required true}])

(defn description [fork-args]
  [form/text-field fork-args
   {:name :description
    :label "description"
    :multiline true
    :maxRows 3
    :minRows 3}])

(defn currency-field [{:keys [values set-values] :as fork-args}
                      {:keys [name inner-config] :as config}]
  (assert name "Missing required arg :name")
  [mui/stack {:spacing 1 :direction :row}
   [form/text-field fork-args
    (merge (dissoc config :inner-config)
           {:InputProps
            {:start-adornment
             (r/as-element
               [mui/input-adornment {:position :start} "£"])}})]
   [mui/form-control-label
    (merge {:label "debt?"
            :checked (-> (get values name) (or "") (form/starts-with? "-"))
            :on-click #(set-values {name (-> (get values name) form/toggle-negative)})
            :control (r/as-element [mui/switch])}
           inner-config)]])

(defn estimated-value [fork-args]
  [currency-field fork-args
   {:name :estimated-value
    :label "estimated value"
    :full-width true}])

(defn sold [{:keys [values handle-change]}]
  [mui/form-control-label
   {:label "sold?"
    :control
    (r/as-element
      [mui/checkbox
       {:name :sold
        :value (:sold values false)
        :checked (:sold values false)
        :on-change #(handle-change %)}])}])

(defn sold-by [fork-args]
  [form/text-field fork-args
   {:name :sold-by
    :label "sold by"}])

(defn confirmed-value [fork-args]
  [currency-field fork-args
   {:name :confirmed-value
    :label "confirmed value"
    :full-width true
    :required true}])

(defn submit-buttons [{:keys [submitting?]}]
  [mui/stack {:spacing 1
              :direction :row}
   [mui/button {:variant :contained
                :full-width true
                :on-click #(rf/dispatch [::model/set-dialog-open])}
    "cancel"]
   [mui/button {:type :submit
                :variant :contained
                :full-width true
                :disabled submitting?}
    "save"]])

(defn layout [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
    [mui/stack {:spacing 1}
     [registration-number fork-args]
     [description fork-args]
     [sold fork-args]
     (if-not (:sold values)
       [estimated-value fork-args]
       [:<>
        [sold-by fork-args]
        [confirmed-value fork-args]])
     [submit-buttons fork-args]]])

(def data-validation
  (v/join
    (v/attr [:registration-number] (v/present))
    (v-util/v-when #(:estimated-value %)
      (v/attr [:estimated-value] (v-util/currency?)))
    (v-util/v-when #(:confirmed-value %)
      (v/attr [:confirmed-value] (v/chain
                                   (v-util/v-when #(:sold %)
                                     (v/present))
                                   (v-util/currency?))))))

(defn form [values on-submit]
  [form/form {:validation #(v/field-errors data-validation %)
              :on-submit on-submit
              :initial-values (or values {})}
   layout])
