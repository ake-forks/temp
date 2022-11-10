(ns darbylaw.web.ui.settings
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.user-details :as user-details]
            [darbylaw.web.ui.deceased-details :as deceased-details]
            [darbylaw.web.ui.app-layout :as c]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [reagent.core :as r]))

(defn menu [panel-k]
  (let [case-id @(rf/subscribe [::case-model/case-id])]
    [mui/list
     [mui/list-item {:key :back-to-case}
      [mui/button {:variant :outlined
                   :startIcon (r/as-element [ui/icon-arrow-back-sharp])
                   :onClick #(rf/dispatch
                               [::ui/navigate
                                [:dashboard {:case-id case-id}]])}
       "Back to dashboard"]]
     [mui/list-subheader
      "Your case"]
     [mui/list-item {:key :user-details}
      [mui/list-item-button {:selected (= panel-k :user-details-panel)
                             :onClick #(rf/dispatch
                                         [::ui/navigate
                                          [:user-details {:case-id case-id}]])}
       [mui/list-item-text {:primary "Your details"}]]]
     [mui/list-item {:key :deceased-details}
      [mui/list-item-button {:selected (= panel-k :deceased-details-panel)
                             :onClick #(rf/dispatch
                                         [::ui/navigate
                                          [:deceased-details {:case-id case-id}]])}
       [mui/list-item-text
        {:primary (if-let [rel @(rf/subscribe [::case-model/relationship])]
                    (str "Your " rel "'s details")
                    "Deceased details")}]]]]))

(defn panel [panel-k]
  [:<>
   [c/navbar]
   [mui/box {:sx {:display :flex}}
    [mui/drawer {:variant :permanent
                 :anchor :left
                 :sx {"& .MuiDrawer-paper" {:position :static}}}
     [mui/toolbar] ; placeholder
     [menu panel-k]]
    [mui/box {:component :main
              :sx {:flex-grow 1}}
     [mui/toolbar] ; placeholder
     (case panel-k
       :user-details-panel [user-details/user-details-panel]
       :deceased-details-panel [deceased-details/panel])]]
   [mui/toolbar]
   [c/footer]])

(defmethod routes/panels :user-details-panel [panel-k]
  [panel panel-k])

(defmethod routes/panels :deceased-details-panel [panel-k]
  [panel panel-k])
