(ns darbylaw.web.ui.vehicle.tasks
  (:require
    [darbylaw.web.ui.vehicle.model :as model]
    [darbylaw.web.ui.app-layout :as layout]
    [re-frame.core :as rf]))

(defn vehicle-tasks []
  (let [vehicles @(rf/subscribe [::model/vehicles])]
    [:<>
     (when (empty? vehicles)
       [layout/task-item
        {:title "add some vehicles"
         :body "if there are any vehicles that are part of the estate, please add them"
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::model/set-dialog-open :add])}])
     (for [{:keys [vehicle-id registration-number sold]}
           vehicles]
       (when (not sold)
         ^{:key vehicle-id}
         [layout/task-item
          {:title (str "complete the sale of " registration-number)
           :body (str "once " registration-number " has been sold, please update our information")
           :icon-path layout/task-icon
           :on-click #(rf/dispatch [::model/set-dialog-open vehicle-id])}]))]))
