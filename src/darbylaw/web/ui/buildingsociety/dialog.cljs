(ns darbylaw.web.ui.buildingsociety.dialog
  (:require
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.stage-add :as add]
    [darbylaw.web.ui.buildingsociety.stage-edit :as edit]
    [darbylaw.web.ui.buildingsociety.stage-notify :as notify]
    [darbylaw.web.ui.buildingsociety.stage-valuation :as valuation]
    [darbylaw.web.ui.buildingsociety.stage-complete :as complete]
    [darbylaw.web.ui.buildingsociety.model :as model]))



(defn dialog []
  (let [dialog-data @(rf/subscribe [::model/get-dialog])
        stage (model/get-process-stage)]
    [mui/dialog
     {:open (or (:open dialog-data) false)
      :maxWidth false
      :fullWidth false
      :scroll :paper}
     (if (some? stage)
       (case stage
         ;dialog-content and dialog-action-area are within each respective panel,
         ;so that the body can be scrolled and action buttons are fixed to the bottom edge

         ;add a new build soc
         :add
         [add/panel]

         ;editing stage
         :edit
         [edit/panel]

         ;approve notification letter and trigger mailing process
         :notify
         [notify/panel]

         ;upload pdf of letter of valuation received from bank and confirm account values
         :valuation
         [valuation/panel]

         ;summary, can view correspondence
         :complete
         [complete/panel]))]))




