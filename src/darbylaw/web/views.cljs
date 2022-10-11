(ns darbylaw.web.views
  (:require
    [re-frame.core :as re-frame]
    [darbylaw.web.routes :as routes]
    [reagent-mui.styles :as mui-styles]
    [reagent-mui.components :as mui]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.deceased-details]
    [darbylaw.web.ui.admin]
    [darbylaw.web.ui.case]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::routes/active-panel])]
    [mui-styles/theme-provider
     (mui-styles/create-theme theme/theme)
     [mui/css-baseline]
     (routes/panels @active-panel)]))
