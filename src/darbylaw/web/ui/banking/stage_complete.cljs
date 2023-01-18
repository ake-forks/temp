(ns darbylaw.web.ui.banking.stage-complete
  (:require
    [darbylaw.web.ui.banking.shared :as shared]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.document-view :as pdf-view]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]))


(defn pdf-panel []
  (let [asset-id @(rf/subscribe [::model/current-banking-id])
        case-id @(rf/subscribe [::case-model/case-id])
        type @(rf/subscribe [::model/current-banking-type])]
    [mui/stack {:spacing 1}
     ;TODO add some kind of loading filler
     [mui/typography {:variant :h6} "correspondence"
      [pdf-view/view-pdf-dialog
       {:buttons
        [{:name "notification letter sent"
          :source (str "/api/case/" case-id "/" (name type) "/" (name asset-id) "/notification-pdf")}
         {:name "valuation letter received"
          :source (str "/api/case/" case-id "/" (name type) "/" (name asset-id) "/valuation-pdf")}]}]]]))

(defn summary-panel []
  (let [asset-id @(rf/subscribe [::model/current-banking-id])
        type @(rf/subscribe [::model/current-banking-type])
        asset-data @(rf/subscribe [::model/current-asset-data])
        relationship @(rf/subscribe [::case-model/relationship])]
    [mui/stack {:spacing 1}
     [mui/typography {:variant :h6} "summary"]
     [mui/typography {:variant :body1}
      (str
        "Here is a summary of your late "
        relationship
        "'s accounts with "
        (model/asset-label type asset-id)
        ". Using the buttons on the left you can view all the correspondence
        sent and received in relation to the following accounts.")]
     (case type
       :bank
       [shared/bank-accounts-view (:accounts asset-data) {:estimated? false :confirmed? true}]
       :buildsoc
       [shared/buildsoc-accounts-view (:accounts asset-data) {:estimated? false :confirmed? true}])]))


(defn panel []
  (let [asset-id @(rf/subscribe [::model/current-banking-id])
        pdf-view @(rf/subscribe [::pdf-view/pdf-view])
        type @(rf/subscribe [::model/current-banking-type])]
    [mui/box
     [mui/dialog-title
      [shared/header type asset-id :complete]]
     [mui/dialog-content
      [mui/box (if (nil? pdf-view)
                 shared/narrow-dialog-props
                 shared/tall-dialog-props)
       [mui/stack {:direction :row :spacing 2}
        ;left side
        [mui/box {:sx {:width 0.5}}
         (if (some? asset-id)
           [pdf-panel])]

        ;right side
        [mui/box {:sx {:width 0.5}}
         [summary-panel]]]]]]))

