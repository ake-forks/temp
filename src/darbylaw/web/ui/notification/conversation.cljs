(ns darbylaw.web.ui.notification.conversation
  (:require [darbylaw.web.util.date :as date-util]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.ui.notification.model :as model]
            [darbylaw.web.ui.notification.letter :as letter]
            [darbylaw.web.ui.notification.received-letter :as received-letter]
            [darbylaw.web.ui.mailing.letter-commons :as letter-commons]))

(def anchor (r/atom nil))

(defn add-menu []
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
      [mui/list-item-icon [letter/draft-icon]]
      [mui/list-item-text
       "create notification letter"]]
     [mui/menu-item {:on-click #(do (close-menu!)
                                    (rf/dispatch [::model/open-upload-received-letter true]))}
      [mui/list-item-icon [received-letter/letter-icon]]
      [mui/list-item-text
       "upload received letter"]]]))

(defn conversation-list []
  [mui/list {:sx {:py 0}}
   (for [letter (<< ::model/conversation)]
     [:<> {:key (:xt/id letter)}
      (case (:type letter)
        ::model/creating
        [mui/list-item {:sx {:background-color :background.paper}}
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

        :probate.notification-letter
        [mui/list-item {:sx {:background-color :background.paper
                             :cursor :pointer}
                        :onClick #(rf/dispatch [::model/open-letter (:xt/id letter)])}
         [mui/list-item-icon {:sx {:color :unset}}
          [letter/draft-icon]
          #_[mui/badge {:badgeContent "\u2197"
                        :color :primary
                        :sx {"& .MuiBadge-badge" {:font-weight :bold
                                                  :background-color :text.primary}}}
             [ui/icon-mail-outlined]]]
         [mui/list-item-text
          {:primary "notification letter"
           :secondary (letter-commons/letter-state-caption letter)}]
         [mui/list-item-text
          {:secondary (date-util/show-date-local-numeric (:modified-at letter))
           :sx {:flex-grow 0}}]]

        :probate.received-letter
        [mui/list-item {:sx {:background-color :background.paper
                             :cursor :pointer}
                        :onClick #(rf/dispatch [::model/open-letter (:xt/id letter)])}
         [mui/list-item-icon {:sx {:color :unset}}
          [received-letter/letter-icon]]
         [mui/list-item-text
          {:primary "received letter"
           :secondary "received"}]
         [mui/list-item-text
          {:secondary (date-util/show-date-local-numeric (:modified-at letter))
           :sx {:flex-grow 0}}]]

        ; else
        [mui/list-item {:sx {:background-color :background.paper}}
         [mui/list-item-icon [ui/icon-question-mark]]
         [mui/list-item-text
          {:secondary (str "unknown item of type " (:type letter))}]])

      [mui/divider]])])

(defn conversation-panel []
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
       "add"]
      [add-menu]]]
    [mui/dialog-content
     [mui/paper {:variant :outlined
                 :square true
                 :sx {:overflow :auto
                      :height "100%"
                      :background-color :action.disabledBackground}}
      (case (<< ::model/conversation-loading)
        :loading [mui/box {:sx {:mt 2
                                :text-align :center}}
                  [mui/circular-progress]]

        nil (if (seq? (<< ::model/conversation))
              [conversation-list]
              [mui/box {:color :action.disabled
                        :mt 2
                        :text-align :center}
               "no communications yet"]))]]]])

(defn panel []
  (let [open-letter (<< ::model/open-letter)]
    (cond
      open-letter
      (case (:type open-letter)
        :probate.notification-letter [letter/panel]
        :probate.received-letter [received-letter/panel]
        [conversation-panel])

      (<< ::model/upload-received-letter?)
      [received-letter/panel]

      :else
      [conversation-panel])))
