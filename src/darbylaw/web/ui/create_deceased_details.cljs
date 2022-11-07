(ns darbylaw.web.ui.create-deceased-details
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.deceased-details-form :as form]
            [reagent-mui.components :as mui]))

(defmethod routes/panels :create-deceased-details-panel []
  [mui/container {:max-width :sm}
   [mui/typography {:variant :h3
                    :sx {:pt 4 :pb 2}}
    "deceased's details"]
   [form/deceased-details-form :create {}]])
