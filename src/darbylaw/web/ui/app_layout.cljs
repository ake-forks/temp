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
    [mui/app-bar {:sx {:zIndex bring-to-front}}
     [mui/toolbar {:variant :dense :class (styles/navbar)}
      [mui/stack {:direction :row :spacing 2}
       [mui/typography {:variant :h6
                        :sx {:color theme/rich-black}}
        "probate-tree"]
       [mui/button {:variant :outlined
                    :startIcon (r/as-element [ui/icon-arrow-back-sharp])
                    :href (kf/path-for [:admin])}
        "Back to admin"]]
      [mui/button {:start-icon (r/as-element [ui/icon-person-outline])
                   :style {:textTransform :none
                           :fontSize :large}
                   :href (kf/path-for [:user-details {:case-id case-id}])}
       [mui/typography {:variant :body1
                        :sx {:font-weight :bold}}
        nickname]]]
     #_(ui/???_TO_BE_DEFINED_??? "do we replace probate-tree with a logo img? black or colourful?")]))

(defn footer []
  [mui/app-bar {:position :fixed :sx {:top "auto" :bottom 0
                                      :zIndex bring-to-front}}
   [mui/toolbar {:variant :dense :class (styles/footer)}
    [mui/typography {:variant :body1}
     "2022 probate-tree. All rights reserved."]
    [mui/link {:variant :body2
               :underline :none}
     "terms and conditions"]]])
