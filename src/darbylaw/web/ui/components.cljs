(ns darbylaw.web.ui.components
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [reagent.core :as r]))


(defn navbar []
  [mui/app-bar
   [mui/toolbar {:variant :dense :class (styles/navbar)}
    [:img {:src "/images/Probate-tree-narrow.png"}]
    [mui/button {:start-icon (r/as-element [ui/icon-person-outline])} "my account"]]])


(defn footer []
  [mui/app-bar {:position :fixed :sx {:top "auto" :bottom 0}}
   [mui/toolbar {:variant :dense :class (styles/footer)}
    [mui/typography {:variant :p} "2022 probate-tree. All rights reserved."]
    [mui/button {:variant :text} "terms and conditions"]]])


(def list-of-banks
  ["HSBC Holdings"
   "Lloyds Banking Group"
   "Royal Bank of Scotland Group"
   "Barclays"
   "Standard Chartered"
   "Santander UK"
   "Nationwide Building Society"
   "Schroders"
   "Close Brothers Group plc"
   "Coventry Building Society"])