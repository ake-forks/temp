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
        {:title "run the identity checks"
         :body (str "the identity checks aren't run automatically (yet)."
                    " please run them manually")
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::identity-dialog/set-dialog-open {}])}])
     ;; Anything other than a pass or no checks (:unknown) requires manual
     ;; intervention
     (when (and (not= final-result :pass)
                (not= final-result :unknown))
       [layout/task-item
        {:title "manually review identity checks"
         :body (str "the identity checks requires some manual intervention."
                    " please manually review the results")
         :icon-path layout/task-icon
         :on-click #(rf/dispatch [::identity-dialog/set-dialog-open {}])}])]))
