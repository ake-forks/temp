(ns darbylaw.web.views
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.routes :as routes]
    [reagent-mui.styles :as mui-styles]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.theme :as theme]

    [reagent-mui.x.localization-provider :as mui-local]
    ["@mui/x-date-pickers/AdapterDayjs" :refer [AdapterDayjs]]
    ["dayjs/locale/en-gb"]

    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.create-deceased-details]
    [darbylaw.web.ui.admin]
    [darbylaw.web.ui.landing]
    [darbylaw.web.ui.history]
    [darbylaw.web.ui.bank-add]
    [darbylaw.web.ui.bank-view]
    [darbylaw.web.ui.bank-confirmation-view]
    [darbylaw.web.ui.dashboard]
    [darbylaw.web.ui.settings]))

(defn add-panel-suffix [k]
  (keyword (str (name k) "-panel")))

(rf/reg-sub ::active-panel
  (fn [db _]
    (if-some [route (-> db :kee-frame/route :data :name)]
      (add-panel-suffix route)
      :waiting-for-routes)))

(defn main-panel []
  (let [active-panel (rf/subscribe [::active-panel])]
    [mui-styles/theme-provider
     (ui/create-theme theme/theme)
     [mui/css-baseline]
     [mui-local/localization-provider {:dateAdapter AdapterDayjs
                                       :adapterLocale "en-gb"}
      (routes/panels @active-panel)]]))
