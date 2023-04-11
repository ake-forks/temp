(ns darbylaw.web.ui.settings
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.user-details :as user-details]
            [darbylaw.web.ui.deceased-details :as deceased-details]
            [darbylaw.web.ui.app-layout :as c]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [darbylaw.web.ui.case-model :as case-model]
            [reagent.core :as r]))

(defn menu [panel-k]
  (let [case-id @(rf/subscribe [::case-model/case-id])]
    [mui/list
     [mui/list-item {:key :back-to-case}
      [mui/button {:variant :outlined
                   :startIcon (r/as-element [ui/icon-arrow-back-sharp])
                   :href (kf/path-for [:dashboard {:case-id case-id}])}
       "Back to dashboard"]]
     [mui/list-subheader
      "Your case"]
     [mui/list-item {:key :user-details}
      [mui/list-item-button {:selected (= panel-k :user-details-panel)
                             :href (kf/path-for
                                     [:user-details {:case-id case-id}])}
       [mui/list-item-text {:primary "Your details"}]]]
     [mui/list-item {:key :deceased-details}
      [mui/list-item-button {:selected (= panel-k :deceased-details-panel)
                             :href (kf/path-for
                                     [:deceased-details {:case-id case-id}])}
       [mui/list-item-text
        {:primary (if-let [rel @(rf/subscribe [::case-model/relationship])]
                    (str "Your " rel "'s details")
                    "Deceased details")}]]]]))

(defn panel [panel-k]
  [:<>
   [c/navbar]
   [mui/stack {:height "100vh"}
    [c/navbar-placeholder]
    [mui/box {:flex-grow 1
              :sx {:overflow :auto}}
     [mui/stack {:direction :row
                 :sx {:max-height 1}}
      [mui/drawer {:variant :permanent
                   :anchor :left
                   :sx {"& .MuiDrawer-paper" {:position :static}}}
       [menu panel-k]]
      [mui/box {:component :main
                :flex-grow 1
                :sx {:width 1
                     :overflow :auto}}
       (case panel-k
         :user-details-panel [user-details/user-details-panel]
         :deceased-details-panel [deceased-details/panel])]]]
    [c/footer-placeholder]]
   [c/footer]])

(defmethod routes/panels :user-details-panel [panel-k]
  [panel panel-k])

(defmethod routes/panels :deceased-details-panel [panel-k]
  [panel panel-k])
