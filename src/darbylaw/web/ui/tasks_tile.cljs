(ns darbylaw.web.ui.tasks-tile
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [reagent.core :as r]
    [darbylaw.web.theme :as theme]))

(defn tasks-item [{:keys [title body] :as task}]
  [mui/stack {:direction :row
              :spacing 1
              :style {:margin-top "0.25rem"
                      :margin-bottom "0.5rem"}}
   [mui/box {:style {:align-self :center :margin "0.5rem"}}
    [:img {:src "/images/green.png" :width "30px" :height "30px"}]]
   [mui/stack
    [mui/typography {:variant :h6 :font-weight 600} title]
    [mui/typography {:variant :body1} body]]])



(defn tasks-tile []
  [mui/card {:style {:height "350px" :background-color theme/off-white}}
   [mui/card-content
    [mui/stack {:direction :row :spacing 0.5 :justify-content :space-between}
     [mui/typography {:variant :h5 :sx {:mb 1}}
      "tasks"]
     [mui/button {:variant :text
                  :endIcon (r/as-element [ui/icon-arrow-forwards])}
      [mui/typography {:variant :body1}
       "view all"]]]
    [mui/divider]
    [tasks-item {:title "new message" :body "case created"}]
    [mui/divider]]])


