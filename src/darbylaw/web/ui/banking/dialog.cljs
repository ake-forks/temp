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
        stage (model/get-process-stage)
        default-props {:open (:open dialog-data)
                       :maxWidth false
                       :scroll :paper}]
    (if (some? stage)
      (case stage
        ;dialog-content and dialog-action-area are within each respective panel,
        ;so that the body can be scrolled and action buttons are fixed to the bottom edge

        ;add a new build soc
        :add
        [mui/dialog default-props
         [add/panel]]

        ;editing stage
        :edit
        [mui/dialog default-props

         [edit/panel]]

        ;approve notification letter and trigger mailing process
        :notify
        [mui/dialog (merge default-props
                      {:maxWidth :xl
                       :fullWidth true})
         [notify/panel]]

        ;upload pdf of letter of valuation received from bank and confirm account values
        :valuation
        [mui/dialog default-props
         [valuation/panel]]

        ;summary, can view correspondence
        :complete
        [mui/dialog default-props
         [complete/panel]]))))
