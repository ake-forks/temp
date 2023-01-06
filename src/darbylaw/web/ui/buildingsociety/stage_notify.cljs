(ns darbylaw.web.ui.buildingsociety.stage-notify
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))



(def approved? (r/atom false))
(defn submit-buttons [case-id buildsoc-id]
  ;TODO amalgamate with buttons in form ns
  (let [letter-id @(rf/subscribe [::model/notification-letter-id])]
    [mui/stack {:spacing 1
                :direction :row
                :justify-content :space-between
                :sx {:width 1}}
     [mui/button {:onClick #(do (reset! approved? false)
                                (rf/dispatch [::model/hide-dialog]))
                  :variant :contained
                  :full-width true}
      "cancel"]
     [mui/button {:type :submit
                  :variant :contained
                  :disabled (not @approved?)
                  :full-width true
                  :startIcon (r/as-element [ui/icon-send])
                  :on-click #(do (rf/dispatch [::model/approve-notification-letter case-id buildsoc-id letter-id])
                                 (reset! approved? false))}
      "send letter"]]))

(defn control-buttons []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-reference [::case-model/current-case-reference]
        buildsoc-id (:id @(rf/subscribe [::model/get-dialog]))]
    [mui/stack {:direction :row :spacing 1}
     [mui/button {:href (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/notification-docx")
                  :download (str case-reference " - " (name buildsoc-id) " - Building Society notification.docx")
                  :variant :outlined
                  :full-width true
                  :startIcon (r/as-element [ui/icon-download])}
      "download current letter"]
     [shared/upload-button case-id buildsoc-id
      {:variant :outlined
       :full-width true
       :startIcon (r/as-element [ui/icon-upload])}
      "upload replacement"
      "/notification-docx"]
     ;TODO replace "use docx file type" with specific error
     [model/upload-error-snackbar "Please upload a docx file type."]]))

(defn approval []
  [mui/alert {:severity :info}
   [mui/stack
    "The letter will be posted automatically through ordinary mail. "
    "We will wait for an answer from the bank for the final valuation step."]
   [mui/form-control-label
    {:control (r/as-element
                [mui/checkbox
                 {:checked @approved?
                  :on-change #(reset! approved? (not @approved?))}])
     :label "I approve, the letter is ready to be sent."}]])

(defn process-list []
  [mui/list
   [mui/list-item {:disable-padding true}
    [mui/list-item-icon
     [ui/icon-download]]
    [mui/list-item-text "download the generated letter as docx file"]]
   [mui/list-item {:disable-padding true}
    [mui/list-item-icon
     [ui/icon-edit]]
    [mui/list-item-text "edit it in Word - take care to preserve the address format"]]
   [mui/list-item {:disable-padding true}
    [mui/list-item-icon
     [ui/icon-upload]]
    [mui/list-item-text "upload the edited file as a replacement"]]])

(defn approve-notification-panel []
  (let [buildsoc-id @(rf/subscribe [::model/current-buildsoc-id])
        case-id @(rf/subscribe [::case-model/case-id])
        buildsoc-data @(rf/subscribe [::model/current-buildsoc-data])]
    [mui/stack {:spacing 3}
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h5} "review notification letter"]
      [mui/typography {:variant :body1}
       "A notification letter has been generated using a standard template and the data from this case."]
      [mui/typography {:variant :body1}
       "If you would like to make changes, use the controls at the bottom to:"]
      [process-list]
      [mui/typography {:variant :body1}
       (str "Once you are happy with the letter, you can approve it to be posted to " buildsoc-id " via Royal Mail.")]]
     [shared/accounts-view (:accounts (first buildsoc-data)) {:estimated? true :confirmed? false}]
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h6} "edit and approve letter"]
      [control-buttons case-id buildsoc-id]
      [approval]]]))


(defn panel []
  (let [buildsoc-id (:id @(rf/subscribe [::model/get-dialog]))
        case-id @(rf/subscribe [::case-model/case-id])]
    (if (some? buildsoc-id)
      [mui/box shared/tall-dialog-props
       [mui/stack {:spacing 1
                   :direction :row
                   :sx {:height 1}}
        ;left side
        [mui/stack {:spacing 1 :sx {:width 0.5}}
         (if @model/file-uploading?
           [reagent-mui.lab.loading-button/loading-button {:loading true :full-width true}]
           [:iframe {:style {:height "100%"}
                     :src (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/notification-pdf")}])]
        ;right side
        [mui/stack {:spacing 1 :sx {:width 0.5}}
         [mui/dialog-title
          [shared/header buildsoc-id 1]]
         [mui/dialog-content
          [approve-notification-panel]]
         [mui/dialog-actions
          [submit-buttons case-id buildsoc-id]]]]])))