(ns darbylaw.web.ui.pensions.add
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.shared :as shared]
    [darbylaw.web.ui.pensions.form :as form]
    [darbylaw.web.ui :as ui :refer (<<)]))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [dialog (<< ::model/dialog)]
    [:form {:on-submit handle-submit}
     [mui/dialog-content {:style shared/dialog-size}
      [shared/dialog-header "add a private pension"]
      [form/company-select fork-args]
      [form/ni-field fork-args]]
     [mui/dialog-actions
      [mui/button {:type :submit} "submit"]]]))

(defn panel []
  [form/form {:layout layout
              :submit-fn #(print %)}])