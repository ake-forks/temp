(ns darbylaw.web.ui.keydocs.tasks
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.keydocs.model :as model]))

;Currently tasks tile has a line [keydocs-tasks], so will need to require all
; the asset-specific task NS to refer each tasks component.
;Alternatively could all the asset task NSs add to a list/atom (?) of collective tasks and the
; tasks tile just displays all of these. If we used an atom it would have to be recalculated/regenerated on page reload I guess?
; Is there any benefit to this over a task NS with lots of 'requires'?
;Finally, to avoid circular dependency I copied task-item and task-icon to the shared layout NS.
; (Should probably move asset cards there too?)

(defn keydocs-tasks []
  (let [present? #(rf/subscribe [::model/document-present? %])]
    [:<>
     (if (not @(present? :death-certificate))
       [layout/task-item {:title "upload death certificate"
                          :body "please upload a scan of the deceased's death certificate"
                          :icon-path layout/task-icon
                          :on-click #(rf/dispatch [::model/show-dialog])}])
     (if (not @(present? :will))
       [layout/task-item {:title "upload will"
                          :body "please upload a scan of the will if present"
                          :icon-path layout/task-icon
                          :on-click #(rf/dispatch [::model/show-dialog])}])
     (if (not @(present? :grant-of-probate))
       [layout/task-item {:title "upload grant of probate"
                          :body "when received, upload a scan of the grant of probate"
                          :icon-path layout/task-icon
                          :on-click #(rf/dispatch [::model/show-dialog])}])]))

