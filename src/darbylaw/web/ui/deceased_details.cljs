(ns darbylaw.web.ui.deceased-details
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.deceased-details-form :as form]))

(defn panel []
  (r/with-let [case-loaded? (case-model/await-load-case!)]
    [mui/container {:max-width :sm}
     [mui/typography {:variant :h3
                      :sx {:pt 4 :pb 2}}
      "deceased's details"]
     (if @case-loaded?
       [form/deceased-details-form :edit
        {:initial-values (:deceased @(rf/subscribe [::case-model/current-case]))}]
       [mui/circular-progress])]))