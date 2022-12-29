(ns darbylaw.web.ui.buildingsociety.dialog
  (:require
    [darbylaw.web.ui :as ui]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.stage-add :as add]
    [darbylaw.web.ui.buildingsociety.stage-edit :as edit]
    [darbylaw.web.ui.buildingsociety.stage-notify :as notify]
    [darbylaw.web.ui.buildingsociety.stage-valuation :as valuation]
    [darbylaw.web.ui.buildingsociety.model :as model]))



(defn dialog []
  (let [dialog-data @(rf/subscribe [::model/get-dialog])
        stage (model/get-process-stage (:id dialog-data))
        all-buildsocs @(rf/subscribe [::model/building-societies])]
    [mui/dialog
     {:open (or (:open dialog-data) false)
      :maxWidth false
      :fullWidth false
      :scroll :paper}
     (if (some? stage)
       (case stage
         ;add a new build soc
         ;dialog content and action area are within panel
         ;(so that body can be scrolled and action buttons are fixed to the bottom edge)
         :add
         [add/panel]

         ;editing stage
         :edit
         [edit/panel]

         :notify
         [notify/panel]

         :valuation
         [valuation/panel]))]))



