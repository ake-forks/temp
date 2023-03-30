(ns darbylaw.web.ui.pensions.shared
  (:require
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui :refer (<<)]
    [darbylaw.web.ui.pensions.model :as model]))

(defn dialog-header [title]
  [mui/stack {:direction :row
              :justify-content :space-between}
   [mui/typography {:variant :h5} title]
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(def dialog-size {:height "40vh" :width "40vw"})
