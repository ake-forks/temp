(ns darbylaw.web.ui.app-layout
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.theme :as theme]))

(defn bring-to-front [theme]
  (inc (.. theme -zIndex -drawer)))

(defn navbar []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        nickname @(rf/subscribe [::case-model/nickname])]
    [mui/app-bar {:sx {:zIndex bring-to-front}
                  :class (styles/navbar)}
     [mui/container {:max-width :xl}
      [mui/toolbar {:variant :dense}
       [mui/stack {:direction :row :spacing 2}
        [mui/link {:variant :h6
                   :sx {:color theme/rich-black}
                   :underline :none
                   :href (kf/path-for [:dashboard {:case-id case-id}])}
         "probate-tree"]
        [mui/button {:variant :outlined
                     :startIcon (r/as-element [ui/icon-arrow-back-sharp])
                     :href (kf/path-for [:admin])}
         "Back to admin"]]
       [mui/box {:style {:flex-grow 1}}]
       [mui/button {:start-icon (r/as-element [ui/icon-person-outline])
                    :style {:textTransform :none
                            :fontSize :large}
                    :href (kf/path-for [:user-details {:case-id case-id}])}
        [mui/typography {:variant :body1
                         :sx {:font-weight :bold}}
         nickname]]
       #_(ui/???_TO_BE_DEFINED_??? "do we replace probate-tree with a logo img? black or colourful?")]]]))

(def task-icon "/images/green.png")
(defn task-item [{:keys [title body icon-path on-click href]}]
  [:<>
   [mui/card-action-area {:on-click on-click :href href}
    [mui/stack {:direction :row
                :spacing 1
                :style {:margin-top "0.25rem"
                        :margin-bottom "0.5rem"}}
     [mui/box {:style {:align-self :center :margin "0.5rem"}}
      [:img {:src icon-path :width "30px" :height "30px"}]]
     [mui/stack
      [mui/typography {:variant :h6 :font-weight 600} title]
      [mui/typography {:variant :body1} body]]]]
   [mui/divider]])

(defn footer []
  [:<>
   [mui/app-bar {:position :fixed
                 :sx {:top "auto" :bottom 0
                      :zIndex bring-to-front}
                 :class (styles/footer)}
    [mui/container {:max-width :xl}
     [mui/toolbar {:variant :dense}
      [mui/typography {:variant :body1}
       "2022 probate-tree. All rights reserved."]
      [mui/box {:style {:flex-grow 1}}]
      [mui/link {:variant :body2
                 :underline :none}
       "terms and conditions"]]]]
   ;; Fixes footer obscuring content at the bottom of the page
   ;; See https://mui.com/material-ui/react-app-bar/#fixed-placement
   [mui/toolbar]]) 
