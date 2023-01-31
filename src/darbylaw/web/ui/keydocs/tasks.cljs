(ns darbylaw.web.ui.keydocs.tasks
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.keydocs.model :as model]))

(defn task-props [title body]
  {:title title
   :body body
   :icon-path layout/task-icon
   :on-click #(rf/dispatch [::model/show-dialog])})

(defn keydocs-tasks []
  (let [present? #(rf/subscribe [::model/document-present? %])]
    [:<>
     (if (not @(present? :death-certificate))
       [layout/task-item (task-props
                           "upload death certificate"
                           "please upload a scan of the deceased's death certificate")])
     (if (not @(present? :will))
       [layout/task-item (task-props
                           "upload will"
                           "please upload a scan of the will if present")])
     (if (not @(present? :grant-of-probate))
       [layout/task-item (task-props
                           "upload grant of probate"
                           "when received, upload a scan of the grant of probate")])]))