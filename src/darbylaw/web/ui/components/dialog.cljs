(ns darbylaw.web.ui.components.dialog
  (:require [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]))


(defn title [{:keys [on-click-close]} & children]
  [mui/stack {:direction :row
              :justify-content :space-between
              :align-items :flex-start}
   (into
     [mui/dialog-title {:sx {:flex-shrink 1
                             :pr 0}}]
     children)
   [mui/icon-button {:onClick on-click-close
                     :sx {:flex-shrink 0}}
    [ui/icon-close]]])
