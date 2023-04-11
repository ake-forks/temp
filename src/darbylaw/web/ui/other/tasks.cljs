(ns darbylaw.web.ui.other.tasks
  (:require
    [darbylaw.web.ui.other.model :as model]
    [darbylaw.web.ui.app-layout :as layout]
    [re-frame.core :as rf]))

(defn other-asset-tasks []
  (let [assets @(rf/subscribe [::model/assets])]
    [:<>
     (when (empty? assets)
       [layout/task-item
        {:title "add some chattel"
         :body "if there are any personal items of value or debt that do not fit into the other categories, please add them"
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::model/set-dialog-open :add])}])]))
