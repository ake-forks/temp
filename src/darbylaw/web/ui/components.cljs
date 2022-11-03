(ns darbylaw.web.ui.components
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [reagent.core :as r]
    [darbylaw.web.theme :as theme]))


(defn navbar []
  [mui/app-bar
   [mui/toolbar {:variant :dense :class (styles/navbar)}
    #_[:img {:src "/images/Probate-tree-narrow.png"}]
    [mui/typography {:variant :h5 :style {:color theme/rich-black}} "probate-tree"]
    [mui/button {:start-icon (r/as-element [ui/icon-person-outline])} "my account"]]])

(defn footer []
  [mui/app-bar {:position :fixed :sx {:top "auto" :bottom 0}}
   [mui/toolbar {:variant :dense :class (styles/footer)}
    [mui/typography {:variant :p} "2022 probate-tree. All rights reserved."]
    [mui/button {:variant :text} "terms and conditions"]]])
