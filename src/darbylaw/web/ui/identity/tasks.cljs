(ns darbylaw.web.ui.identity.tasks
  (:require
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.identity.dialog :as identity-dialog]
    [darbylaw.web.ui.identity.model :as model]
    [re-frame.core :as rf]))

(defn identity-tasks []
  (let [final-result @(rf/subscribe [::model/current-final-result])]
    [:<>
     (when (= final-result :unknown)
       [layout/task-item
        {:title "run identity checks"
         :body "please run the identity checks"
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::identity-dialog/set-dialog-open {}])}])
     ;; Anything other than a pass or no checks (:unknown) requires manual
     ;; intervention
     (when (and (not= final-result :pass)
                (not= final-result :unknown)
                (nil? @(rf/subscribe [::model/override-result])))
       [layout/task-item
        {:title "review identity checks"
         :body (str "manual intervention required")
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::identity-dialog/set-dialog-open {}])}])]))
