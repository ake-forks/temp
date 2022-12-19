(ns darbylaw.web.ui.buildingsociety.shared
  (:require
    [reagent-mui.components :as mui]))

(defn stepper []
  [mui/stepper {:alternative-label true}
   [mui/step
    [mui/step-label "ADD ACCOUNTS"]]
   [mui/step
    [mui/step-label "SEND NOTIFICATION"]]
   [mui/step
    [mui/step-label "CONFIRM VALUES"]]])