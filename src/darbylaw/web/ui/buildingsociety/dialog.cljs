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
        stage (:stage dialog-data)]
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
         []))]))


