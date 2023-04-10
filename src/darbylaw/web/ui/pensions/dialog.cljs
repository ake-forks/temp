(ns darbylaw.web.ui.pensions.dialog
  (:require
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.add :as add]
    [darbylaw.web.ui :as ui :refer (<<)]))


(defn dialog []
  (let [dialog (<< ::model/dialog)
        default-props {:open (or (:open dialog) false)
                       :maxWidth false
                       :scroll :paper}]
    (when (:open dialog)
      (case (:dialog-type dialog)
        :add
        [mui/dialog default-props
         [add/panel]]))))