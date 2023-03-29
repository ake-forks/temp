(ns darbylaw.web.ui.pensions.dashboard
  (:require
    [darbylaw.web.ui.app-layout :as l]
    [darbylaw.web.ui.pensions.model :as model]))

(defn pensions-card []
  [:<>
   [l/asset-card {:title "pensions"}
    [l/menu-asset-add-button
     model/anchor
     {"add state pension" #(print "state")
      "add private pension" #(print "private")}]]])
