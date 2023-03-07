(ns darbylaw.web.ui.identity.dialog
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.identity.model :as model]
    [darbylaw.web.ui.identity.dialog.right :as right]
    [darbylaw.web.ui.identity.dialog.left :as left]
    [re-frame.core :as rf]))

(defn dialog-content []
  [mui/dialog-content
   [mui/stack {:spacing 3
               :direction :row}
    [left/panel]
    [right/panel]]])

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::model/dialog-open?]))
               :max-width :md
               :full-width true}
   [mui/backdrop {:open (boolean @(rf/subscribe [::model/submitting?]))}
    [mui/circular-progress]]
   [mui/stack {:spacing 1}
    [mui/dialog-title
     [mui/stack {:spacing 1 :direction :row}
      "identity checks"
      [mui/box {:flex-grow 1}]
      [ui/icon-close {:style {:cursor :pointer}
                      :on-click #(rf/dispatch [::model/set-dialog-open])}]]]
    [dialog-content]]])
