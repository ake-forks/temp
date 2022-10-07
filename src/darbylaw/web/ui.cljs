(ns darbylaw.web.ui
  (:require
    [reagent-mui.icons.account-balance]
    [reagent-mui.icons.add]
    [reagent-mui.icons.help-outline]
    [reagent-mui.icons.add-circle]
    [reagent-mui.icons.person-outline]
    [reagent-mui.components :as mui]
    [reagent.core :as r]))

(def icon-add reagent-mui.icons.add/add)
(def icon-help-outline reagent-mui.icons.help-outline/help-outline)
(def icon-add-circle reagent-mui.icons.add-circle/add-circle)
(def icon-account-balance reagent-mui.icons.account-balance/account-balance)
(def icon-person-outline reagent-mui.icons.person-outline/person-outline)

(defn ???_TO_BE_DEFINED_??? [message]
  [mui/alert {:severity :warning
              :icon (r/as-element [icon-help-outline {:fontSize :inherit}])}
   message])