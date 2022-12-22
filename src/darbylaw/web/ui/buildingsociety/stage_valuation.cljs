(ns darbylaw.web.ui.buildingsociety.stage-valuation
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))


(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        dialog-data @(rf/subscribe [::model/get-dialog])]
    [:form {:on-submit handle-submit}]
    [mui/box shared/tall-dialog-props
     [mui/stack {:spacing 1
                 :direction :row
                 :sx {:height 1}}
      ;left side
      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [:iframe {:style {:height "100%"}
                 :src "/Example-bank-confirmation-letter.pdf"}]]

      ;right side
      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [mui/dialog-title
        [shared/header (:id dialog-data) 2]]
       [mui/dialog-content
        [mui/stack {:spacing 2}
         [mui/typography {:variant :body1}
          (str "Once you have received a letter from "
            (:id dialog-data)
            ", upload a scanned pdf using the button below.")]
         [mui/button {:variant :outlined} "upload pdf"]
         [mui/typography {:variant :body1}
          (str "Please enter the confirmed details for each of your late "
            (-> current-case :deceased :relationship)
            "'s accounts with "
            (:id dialog-data) ".")]
         [form/account-array-component (merge fork-args {:stage :valuation})]]]
       [mui/dialog-actions
        [form/submit-buttons]]]]]))

(defn panel []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        case-id (:id current-case)]
    [form/form layout {:accounts [{}]} #(print %)]))