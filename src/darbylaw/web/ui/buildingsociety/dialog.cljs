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
      :fullWidth false}
     (case stage
       :add
       [mui/box {:style {:height "70vh"
                         :width "70vw"}}
        [shared/stepper]
        [edit/panel]]



       2
       [mui/box {:style {:height "90vh"
                         :width "90vw"}}])]))

