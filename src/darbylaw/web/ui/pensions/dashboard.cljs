(ns darbylaw.web.ui.pensions.dashboard
  (:require
    [darbylaw.web.ui.app-layout :as l]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.dialog :as dialog]
    [re-frame.core :as rf]))

(defn pensions-card []
  [:<>
   [dialog/dialog]
   [l/asset-card {:title "pensions"}
    [l/menu-asset-add-button
     model/anchor
     {"add state pension" #(print "state")
      "add private pension" #(rf/dispatch [::model/show-dialog nil :private :add])}]]])
