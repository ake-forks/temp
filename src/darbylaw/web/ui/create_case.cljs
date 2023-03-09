(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.user-details-form :as form]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]))

(defn panel []
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

   [mui/container {:max-width :md
                   :sx {:mb 4}}
    [mui/stack {:spacing 4}
     [mui/stack
      [mui/typography {:variant :h4
                       :sx {:pt 4 :pb 2}}
       "get started"]
      [mui/typography {:variant :body1}
       "It looks like you need probate.
       Here are some quick questions about you.
       Then we will ask about the deceased and their relationship to you."]]
     [form/user-details-form :create]]]])

(defmethod routes/panels :create-case-panel [] [panel])
