(ns darbylaw.web.ui.user-details
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.user-details-form :as form]))

(defn user-details-panel []
  (r/with-let [case-loaded? (case-model/await-load-case!)]
    [mui/container {:max-width :md}
     [mui/typography {:variant :h3
                      :sx {:pt 4 :pb 2}}
      "your details"]
     (if @case-loaded?
       [form/user-details-form :edit
        {:initial-values (:personal-representative
                           @(rf/subscribe [::case-model/current-case]))}]
       [mui/circular-progress])]))
