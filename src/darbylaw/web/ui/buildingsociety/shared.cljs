(ns darbylaw.web.ui.buildingsociety.shared
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]))


(defn close-button-row []
  [mui/stack {:direction :row
              :style {:justify-content :end}}
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(defn close-button []
  [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
   [ui/icon-close]])
(defn stepper [stage]
  [mui/stepper {:alternative-label true :active-step stage}
   [mui/step
    [mui/step-label "ADD ACCOUNTS"]]
   [mui/step
    [mui/step-label "SEND NOTIFICATION"]]
   [mui/step
    [mui/step-label "CONFIRM VALUES"]]])

(defn title [name]
  [mui/typography {:variant :h4} name])

(defn header [name stage]
  [mui/stack {:spacing 2}
   [mui/stack {:direction :row :justify-content :space-between}
    [title name]
    [close-button]]
   [stepper stage]])

(defn submit-buttons []
  [form/submit-buttons])