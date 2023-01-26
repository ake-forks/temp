(ns darbylaw.web.ui.bills.add-form
  (:require [darbylaw.web.util.form :as form-util]
            [darbylaw.web.ui.bills.model :as model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]))

(defn company-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :company
    :label "Company"
    :options @(rf/subscribe [::model/all-company-ids])
    :getOptionLabel @(rf/subscribe [::model/company-id->label])
    :freeSolo true}])

(defn type-of-bill [{:keys [values set-handle-change] :as _fork-args}]
  (let [all-bill-types @(rf/subscribe [::model/bill-types])
        checked-values (get values :bill-type #{})]
    [mui/form-control
     [mui/form-label "Bill type (select all applicable)"]
     [mui/stack {:direction :row}
      (for [bill-types (partition 2 all-bill-types)]
        [mui/stack
         (for [{:keys [name label]} bill-types]
           [mui/form-group
            [mui/form-control-label
             {:control (r/as-element
                         [mui/checkbox
                          {:checked (contains? checked-values name)
                           :onChange (fn [evt]
                                       (set-handle-change
                                         {:value (if (ui/event-target-checked evt)
                                                   (conj checked-values name)
                                                   (disj checked-values name))
                                          :path [:bill-type]}))
                           :name name}])
              :label (if (contains? checked-values name)
                       (r/as-element [:b label])
                       label)}]])])]
     [mui/form-helper-text "helper text"]]))

(defn form [fork-args]
  [mui/stack {:spacing 2}
   [company-select fork-args]
   [type-of-bill fork-args]])
