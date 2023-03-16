(ns darbylaw.web.ui.properties.dialog
  (:require
    [darbylaw.api.util.data :as data-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.properties.model :as model]
    [darbylaw.web.ui.properties.form :as form]
    [darbylaw.web.ui :as ui :refer (<<)]))

(defn dialog-header [title]
  [mui/stack {:direction :row
              :justify-content :space-between}
   [mui/typography {:variant :h5} title]
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(defn edit [prop-id]
  (let [property (model/get-property prop-id)]
     [mui/stack {:spacing 1 :style {:width "40vw"
                                    :height "60vh"}}
      [dialog-header (data-util/first-line (:address property))]
      [mui/typography {:variant :h5} (str property)]
      [mui/dialog-content]]))

(defn add [{:keys [handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/dialog-content {:style {:width "40vw" :height "60vh"}}
    [dialog-header "add property"]
    [mui/stack {:spacing 1}
     [mui/typography {:variant :h6} "details"]
     [form/address-field fork-args]
     [form/joint-owner-field fork-args]
     [mui/typography {:variant :h6} "valuation"]
     [form/value-field fork-args]
     [mui/typography {:variant :h6} "supporting documents"]
     [form/documents-field]]]

   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :on-click #(rf/dispatch [::model/hide-dialog])} "cancel"]
    [mui/button {:variant :outlined :type :submit} "save"]]])

(defn dialog []
  (let [dialog (<< ::model/dialog)
        default-props {:open (or (:open dialog) false)
                       :maxWidth false
                       :scroll :paper}]

    (when (:open dialog)
     (case (:dialog-type dialog)
       :edit
       [mui/dialog default-props
        [edit (:id dialog)]]
       :add
       [mui/dialog default-props
        [form/form {:layout add :submit-fn #(print %)}]]))))



