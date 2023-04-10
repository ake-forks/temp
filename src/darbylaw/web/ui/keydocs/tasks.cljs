(ns darbylaw.web.ui.keydocs.tasks
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui :refer [<<]]
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.keydocs.model :as model]))

(defn death-certificate-task []
  (when-not (<< ::model/document-present? :death-certificate)
    [layout/task-item {:title "upload death certificate"
                       :body "please upload a scan of the deceased's death certificate"
                       :on-click #(rf/dispatch [::model/show-dialog])}]))

(defn keydocs-tasks []
  [:<>
   (when-not (<< ::model/document-present? :will)
     [layout/task-item {:title "upload will"
                        :body "please upload a scan of the will if present"
                        :on-click #(rf/dispatch [::model/show-dialog])}])
   #_(when-not (<< ::model/document-present? :grant-of-probate)
       [layout/task-item {:title "upload grant of probate"
                          :body "when received, upload a scan of the grant of probate"
                          :on-click #(rf/dispatch [::model/show-dialog])}])])
