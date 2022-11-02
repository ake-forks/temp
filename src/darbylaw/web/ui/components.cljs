(ns darbylaw.web.ui.components
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]))

(defn navbar []
  [mui/app-bar
   [mui/toolbar {:variant :dense :class (styles/navbar)}
    [mui/typography {:variant :h5} "probate-tree"]
    [mui/button {:start-icon (r/as-element [ui/icon-person-outline])
                 :style {:textTransform :none}}
     @(rf/subscribe [::case-model/nickname])]]
   #_(ui/???_TO_BE_DEFINED_??? "do we replace probate-tree with a logo img? black or colourful?")])

(defn footer []
  [mui/app-bar {:position :fixed :sx {:top "auto" :bottom 0}}
   [mui/toolbar {:variant :dense :class (styles/footer)}
    [mui/typography {:variant :p} "2022 probate-tree. All rights reserved."]
    [mui/button {:variant :text} "terms and conditions"]]])


