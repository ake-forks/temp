(ns darbylaw.web.views
  (:require
    [re-frame.core :as re-frame]
    [darbylaw.web.subs :as subs]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.deceased-details]
    [darbylaw.web.ui.admin]))

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (routes/panels @active-panel)))
