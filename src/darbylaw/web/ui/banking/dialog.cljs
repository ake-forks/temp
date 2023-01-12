(ns darbylaw.web.ui.banking.dialog
  (:require
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.stage-add :as add]
    [darbylaw.web.ui.banking.stage-edit :as edit]
    [darbylaw.web.ui.banking.stage-notify :as notify]
    [darbylaw.web.ui.banking.stage-valuation :as valuation]
    [darbylaw.web.ui.banking.stage-complete :as complete]
    [darbylaw.web.ui.banking.model :as model]))



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




