(ns darbylaw.web.ui.bills.add-dialog
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui.bills.model :as model]))

(defn company-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :company
    :label "Company"
    :options @(rf/subscribe [::model/all-company-ids])
    :getOptionLabel @(rf/subscribe [::model/company-id->label])
    :freeSolo true}])

(rf/reg-event-db ::open
  (fn [db [_ show?]]
    (assoc db ::open? show?)))

(rf/reg-sub ::open?
  (fn [db]
    (-> db ::open?)))

(def form-state (r/atom nil))

(defn form []
  [form-util/form {:form-state form-state}
   (fn [fork-args]
     [:<>
      [mui/dialog-content
       [company-select fork-args]]
      [mui/dialog-actions
       [mui/button {:variant :contained}
        "Add"]
       [mui/button {:variant :outlined
                    :onClick #(rf/dispatch [::open false])}
        "Cancel"]]])])

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::open?]))}
   [mui/dialog-title
    "add household bill"
    [mui/icon-button {:onClick #(rf/dispatch [::open false])}
     [ui/icon-close]]]
   [form]])

(defn show []
  (rf/dispatch [::open true]))
