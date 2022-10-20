(ns darbylaw.web.views
  (:require
    [re-frame.core :as re-frame]
    [darbylaw.web.routes :as routes]
    [reagent-mui.styles :as mui-styles]
    [reagent-mui.components :as mui]
    [darbylaw.web.theme :as theme]

    [reagent-mui.x.localization-provider :as mui-local]
    ["@mui/x-date-pickers/AdapterDayjs" :refer [AdapterDayjs]]
    ["dayjs/locale/en-gb"]

    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.deceased-details]
    [darbylaw.web.ui.admin]
    [darbylaw.web.ui.case]
    [darbylaw.web.ui.dashboard]
    [darbylaw.web.ui.bank]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::routes/active-panel])]
    [mui-styles/theme-provider
     (mui-styles/create-theme theme/theme)
     [mui/css-baseline]
     [mui-local/localization-provider {:dateAdapter AdapterDayjs
                                       :adapterLocale "en-gb"}
      (routes/panels @active-panel)]]))
