(ns darbylaw.web.ui.notification.conversation
  (:require [darbylaw.web.util.date :as date-util]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.ui.notification.model :as model]))

(def anchor (r/atom nil))

(defn create-menu []
  (let [close-menu! #(reset! anchor nil)
        notification (<< ::model/notification)]
    [mui/menu {:open (some? @anchor)
               :anchorEl @anchor
               :onClose close-menu!
               :anchorOrigin {:vertical :bottom
                              :horizontal :right}
               :transformOrigin {:vertical :top
                                 :horizontal :right}
               :PaperProps {:sx {:border-radius 0}}}
     [mui/menu-item {:on-click #(do (close-menu!)
                                    (rf/dispatch [::model/generate-notification-letter
                                                  notification]))}
      [mui/list-item-text
       "create notification letter"]]
     [mui/menu-item {:on-click close-menu!}
      [mui/list-item-text
       "upload received letter"]]]))

(defn panel []
  (let [conversation (<< ::model/conversation)]
    [mui/container {:maxWidth :sm
                    :sx {:height "100%"}}
     [mui/stack {:sx {:height "100%"}}
      [mui/dialog-title
       [mui/stack {:direction :row
                   :sx {:justify-content :space-between}}
        "conversation"
        [mui/button {:startIcon (r/as-element [ui/icon-add])
                     :variant :contained
                     :onClick #(reset! anchor (ui/event-currentTarget %))}
         "create"]
        [create-menu]]]
      [mui/dialog-content
       [mui/paper {:variant :outlined
                   :square true
                   :sx {:overflow :auto
                        :height "100%"
                        :background-color :action.disabledBackground}}
        (case conversation
          :loading [mui/box {:sx {:mt 2
                                  :text-align :center}}
                    [mui/circular-progress]]

          nil [mui/box {:color :action.disabled
                        :mt 2
                        :text-align :center}
               "no communications yet"]

          [mui/list {:sx {:py 0}}
           (for [letter conversation]
             [:<> {:key (:xt/id letter)}
              (case (:type letter)
                ::model/creating [mui/list-item {:sx {:background-color :background.paper}}
                                  [mui/list-item-icon
                                   [mui/skeleton {:variant :circular
                                                  :width "1.5em"
                                                  :height "1.5em"}]]
                                  [mui/list-item-text
                                   {:primary (r/as-element [mui/skeleton {:width 180}])
                                    :secondary (r/as-element [mui/skeleton {:width 120}])}]
                                  [mui/list-item-text
                                   {:secondary (r/as-element [mui/skeleton {:width 80}])
                                    :sx {:flex-grow 0}}]]

                [mui/list-item {:sx {:background-color :background.paper
                                     :cursor :pointer}
                                :onClick #(rf/dispatch [::show-letter (:xt/id letter)])}
                 [mui/list-item-icon
                  [ui/icon-description-outlined]]
                 [mui/list-item-text
                  {:primary "notification letter"
                   :secondary "in preparation"}]
                 [mui/list-item-text
                  {:secondary (date-util/show-date-local-numeric (:modified-at letter))
                   :sx {:flex-grow 0}}]])

              [mui/divider]])])]]]]))
