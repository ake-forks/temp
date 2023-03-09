(ns darbylaw.web.ui.identity.dialog.utils
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.identity.model :as model]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn check-icon
  ([]
   [check-icon @(rf/subscribe [::model/current-final-result])])
  ([result]
   (r/as-element
     [:a {:style {:line-height 0
                  :cursor :pointer}
          :on-click #(rf/dispatch [::model/set-dialog-open {}])}
      (case result
        :unknown [ui/icon-playlist-play {:style {:color "grey"}}]
        :processing [ui/icon-pending {:style {:color "grey"}}]
        :pass [ui/icon-check {:style {:color "green"}}]
        :refer [ui/icon-warning-amber {:style {:color "orange"}}]
        :fail [ui/icon-warning {:style {:color "red"}}])])))
