(ns darbylaw.web.ui.buildingsociety.stage-complete
  (:require
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.document-view :as pdf-view]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]))


(defn pdf-panel []
  (let [buildsoc-id (:id @(rf/subscribe [::model/get-dialog]))
        case-id @(rf/subscribe [::case-model/case-id])]
    [mui/stack {:spacing 1}
     ;TODO add some kind of loading filler
     [mui/typography {:variant :h6} "correspondence"
      [pdf-view/view-pdf-dialog
       {:buttons
        [{:name "notification letter sent"
          :source (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/notification-pdf")}
         {:name "valuation letter received"
          :source (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/valuation-pdf")}]}]]]))
(defn summary-panel []
  (let [buildsoc-id (:id @(rf/subscribe [::model/get-dialog]))
        buildsoc-data @(rf/subscribe [::model/current-buildsoc-data])
        relationship @(rf/subscribe [::case-model/relationship])]
    [mui/stack {:spacing 1}
     [mui/typography {:variant :h6} "summary"]
     [mui/typography {:variant :body1}
      (str
        "Here is a summary of your late "
        relationship
        "'s accounts with "
        (model/buildsoc-label buildsoc-id)
        ". Using the buttons on the left you can view all the correspondence
        sent and received in relation to the following accounts.")]
     [shared/accounts-view (:accounts buildsoc-data) {:estimated? false :confirmed? true}]]))


(defn panel []
  (let [buildsoc-id (:id @(rf/subscribe [::model/get-dialog]))
        pdf-view @(rf/subscribe [::pdf-view/pdf-view])]
    [mui/box
     [mui/dialog-title
      [shared/header buildsoc-id :complete]]
     [mui/dialog-content
      [mui/box (if (nil? pdf-view)
                 shared/narrow-dialog-props
                 shared/tall-dialog-props)
       [mui/stack {:direction :row :spacing 2}
        ;left side
        [mui/box {:sx {:width 0.5}}
         (if (some? buildsoc-id)
           [pdf-panel])]

        ;right side
        [mui/box {:sx {:width 0.5}}
         [summary-panel]]]]]]))

