(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [kee-frame.core :as kf]
            [darbylaw.web.ui.user-details-form :as form]
            [reagent-mui.components :as mui]))

(defn panel []
  [mui/container {:max-width :sm
                  :sx {:mb 4}}
   [mui/stack {:spacing 4}
    [mui/stack
     [mui/typography {:variant :h3
                      :sx {:pt 4 :pb 2}}
      "get started"]
     [mui/typography {:variant :p}
      "It looks like you need probate.
      Here are some quick questions about you.
      Then we will ask about the deceased and their relationship to you."]]
    [form/personal-info-form :create]]])

(defmethod routes/panels :create-case-panel [] [panel])
