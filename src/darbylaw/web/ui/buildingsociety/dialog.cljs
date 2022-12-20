(ns darbylaw.web.ui.buildingsociety.dialog
  (:require
    [darbylaw.web.ui :as ui]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.stage-add :as add]
    [darbylaw.web.ui.buildingsociety.stage-edit :as edit]
    [darbylaw.web.ui.buildingsociety.model :as model]))



(defn dialog []
  (let [dialog-data @(rf/subscribe [::model/get-dialog])
        stage :add]
    [mui/dialog
     {:open (or (:open dialog-data) false)
      :maxWidth false
      :fullWidth false
      :scroll :paper}
     (case stage

       ;add a new build soc
       :edit
       [:<>
        [mui/dialog-title
         [mui/typography {:variant :h4} "add building society"]]
        [mui/dialog-content
         [mui/box {:style {:height "50vh"
                           :width "60vw"
                           :padding "1rem"}}
          [add/panel]]]
        [mui/dialog-actions
         [shared/submit-buttons]]]

       ;editing stage
       :add
       [:<>
        [mui/dialog-title
         [shared/header "Bath Building Society" 0]]
        [mui/dialog-content
         [mui/box {:style {:height "50vh"
                           :width "60vw"
                           :padding "1rem"}}
          [edit/panel]]]
        [mui/dialog-actions
         [shared/submit-buttons]]]

       2
       [mui/box {:style {:height "90vh"
                         :width "90vw"}}])]))

