(ns darbylaw.web.ui.create-deceased-details
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.deceased-details-form :as form]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]))

(defmethod routes/panels :create-deceased-details-panel []
  [:<>
   [mui/fab {:variant :extended
             :sx {:position :fixed
                  :bottom (ui/theme-spacing 2)
                  :right (ui/theme-spacing 2)
                  :opacity 0.1
                  "&:hover" {:opacity 1}}
             :onClick #(form/dev-auto-fill)}
    [ui/icon-edit {:sx {:mr 1}}]
    "Sample data"]

   [mui/container {:max-width :sm
                   :sx {:mb 4}}
    [mui/typography {:variant :h3
                     :sx {:pt 4 :pb 2}}
     "deceased's details"]
    [form/deceased-details-form :create {}]]])
