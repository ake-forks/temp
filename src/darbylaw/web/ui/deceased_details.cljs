(ns darbylaw.web.ui.deceased-details
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.deceased-details-form :as form]
            [darbylaw.web.ui.wait-case-loaded :as wait-case]))

(defn panel []
  [mui/container {:max-width :sm}
   [mui/typography {:variant :h3
                    :sx {:pt 4 :pb 2}}
    "deceased's details"]
   (if-let [current-case @(rf/subscribe [::wait-case/loaded?])]
     [form/panel :edit
      {:initial-values (:deceased current-case)}]
     [mui/circular-progress])])