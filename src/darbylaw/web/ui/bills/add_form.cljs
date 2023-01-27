(ns darbylaw.web.ui.bills.add-form
  (:require [darbylaw.web.util.form :as form-util]
            [darbylaw.web.ui.bills.model :as model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]))

(defn type-of-bill-choice [{:keys [values set-handle-change submitting?] :as _fork-args}]
  (let [all-bill-types @(rf/subscribe [::model/bill-types])
        checked-values (get values :bill-type #{})]
    [mui/form-control
     [mui/form-label "bill type (select all applicable)"]
     [mui/stack {:direction :row}
      (for [bill-types (partition 2 all-bill-types)]
        ^{:key (pr-str bill-types)}
        [mui/stack
         (for [{:keys [name label]} bill-types]
           ^{:key name}
           [mui/form-group
            [mui/form-control-label
             {:control (r/as-element
                         [mui/checkbox
                          {:checked (contains? checked-values name)
                           :onChange (fn [evt]
                                       (set-handle-change
                                         {:value (if (ui/event-target-checked evt)
                                                   (if (= name :council-tax)
                                                     #{:council-tax}
                                                     (-> checked-values
                                                       (conj name)
                                                       (disj :council-tax)))
                                                   (disj checked-values name))
                                          :path [:bill-type]}))
                           :name name
                           :disabled submitting?}])
              :label (if (contains? checked-values name)
                       (r/as-element [:b label])
                       label)}]])])]
     [mui/form-helper-text "helper text"]]))

(defn company-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :company
    :label "company"
    :options @(rf/subscribe [::model/all-company-ids])
    :getOptionLabel @(rf/subscribe [::model/company-id->label])
    :freeSolo true}])

(defn council-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :council
    :label "council"
    :options @(rf/subscribe [::model/all-council-ids])
    :getOptionLabel @(rf/subscribe [::model/council-id->label])
    :freeSolo true}])

(defn disabled-select-placeholder []
  [mui/text-field {:disabled true}])

(defn account-number-field [fork-args]
  [form-util/text-field fork-args {:name :account-number
                                   :label "account number"}])

(defn address-field [fork-args]
  [form-util/text-field fork-args {:name :address
                                   :label "supply address"
                                   :multiline true
                                   :minRows 3
                                   :maxRows 3}])

(defn amount-field [fork-args]
  [form-util/text-field fork-args
   {:name :amount
    :label "amount"
    :full-width true
    :required true
    :InputProps
    {:start-adornment
     (r/as-element
       [mui/input-adornment {:position :start} "£"])}}])

(defn meter-readings-field [fork-args]
  [form-util/text-field fork-args {:name :meter-readings
                                   :label "meter readings"}])

(defn form [{:keys [values] :as fork-args}]
  [mui/stack {:spacing 2}
   [type-of-bill-choice fork-args]
   (let [bill-type (get values :bill-type)]
     (cond
       (contains? bill-type :council-tax)
       [council-select fork-args]

       (seq bill-type)
       [company-select fork-args]

       :else
       [disabled-select-placeholder]))
   [account-number-field fork-args]
   [address-field fork-args]
   [amount-field fork-args]
   [meter-readings-field fork-args]])
