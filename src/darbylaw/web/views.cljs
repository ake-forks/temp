(ns darbylaw.web.views
  (:require
    [re-frame.core :as re-frame]
    [darbylaw.web.subs :as subs]
    [darbylaw.web.routes :as routes]
    [reagent-mui.styles :as mui-styles]
    [reagent-mui.components :as mui]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.deceased-details]
    [darbylaw.web.ui.admin]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [mui-styles/theme-provider
     (mui-styles/create-theme theme/theme)
     [mui/css-baseline]
     (routes/panels @active-panel)]))
