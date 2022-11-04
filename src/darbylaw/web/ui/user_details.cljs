(ns darbylaw.web.ui.user-details
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.user-details-form :as form]
            [darbylaw.web.ui.wait-case-loaded :as wait-case]))

(defn user-details-panel []
  [mui/container {:max-width :sm}
   [mui/typography {:variant :h3
                    :sx {:pt 4 :pb 2}}
    "your details"]
   (if @(rf/subscribe [::wait-case/loaded?])
     [form/personal-info-form :edit
      {:initial-values (:personal-representative
                         @(rf/subscribe [::case-model/current-case]))}]
     [mui/circular-progress])])
