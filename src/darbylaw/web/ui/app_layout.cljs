(ns darbylaw.web.ui.app-layout
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.theme :as theme]
    [reagent.format :as format]))

(defn navbar-placeholder []
  [mui/toolbar {:variant :dense}])

(defn navbar []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        nickname @(rf/subscribe [::case-model/nickname])]
    [mui/app-bar {:elevation 2}
     [mui/container {:max-width :xl}
      [mui/toolbar {:variant :dense
                    :disableGutters true}
       [mui/stack {:direction :row
                   :spacing 2
                   :align-items :center}
        [mui/link {:variant :h6
                   :sx {:color theme/rich-black}
                   :underline :none
                   :href (kf/path-for [:dashboard {:case-id case-id}])}
         "probate-tree"]
        [mui/button {:variant :outlined
                     :startIcon (r/as-element [ui/icon-admin-panel-settings-outlined])
                     :href (kf/path-for [:admin])}
         "admin"]]
       [mui/box {:style {:flex-grow 1}}]
       [mui/button {:start-icon (r/as-element [ui/icon-person-outline])
                    :style {:textTransform :none
                            :fontSize :large}
                    :href (kf/path-for [:user-details {:case-id case-id}])}
        [mui/typography {:variant :body1
                         :sx {:font-weight :bold}}
         nickname]]
       #_(ui/???_TO_BE_DEFINED_??? "do we replace probate-tree with a logo img? black or colourful?")]]]))

(defn footer-placeholder []
  [mui/toolbar {:variant :dense}])

(defn footer []
  [:<>
   [mui/app-bar {:elevation 2
                 :sx {:top "auto"
                      :bottom 0}}
    [mui/container {:max-width :xl}
     [mui/toolbar {:variant :dense}
      [mui/typography {:variant :body1
                       :color :text.disabled}
       "2022 probate-tree. All rights reserved."]
      [mui/box {:style {:flex-grow 1}}]
      [mui/link {:variant :body2
                 :underline :none
                 :color :text.disabled}
       "terms and conditions"]]]]])

;Dashboard cards
(defn asset-add-button [{:keys [title on-click]}]
  [mui/button {:onClick on-click
               :startIcon (r/as-element [ui/icon-add])
               :size :large
               :sx {:width 1
                    :justify-content :flex-start
                    :fontSize :body1.fontSize}}
   (or title "add")])

(defn asset-card-header [title]
  [:<>
   [mui/typography {:variant :h5
                    :sx {:font-weight 600
                         :mb 1}}
    title]
   [mui/divider]])

(defn asset-card [{:keys [title _on-add]} & body]
  [mui/card {:sx {:border-style :solid
                  :border-width 1
                  :border-color :divider}}
   [mui/card-content {:sx {:paddingTop (ui/theme-spacing 1.5)
                           "&:last-child" {:paddingBottom (ui/theme-spacing 0.5)}}}
    [asset-card-header title]
    (into [:<>] body)]])

(defn menu-asset-add-button
  "anchor = atom
  options = a map of option labels and their on-click functions as key-value pairs
  eg {\"add utility\" #(rf/dispatch...)}"
  [anchor options]
  [:<>
   [asset-add-button
    {:title "add"
     :on-click #(reset! anchor (ui/event-currentTarget %))}]
   [mui/menu {:open (some? @anchor)
              :anchor-el @anchor
              :on-close #(reset! anchor nil)
              :anchor-origin {:vertical "bottom"
                              :horizontal "left"}
              :transform-origin {:vertical "top"
                                 :horizontal "left"}}
    (map (fn [[k v]]
           ^{:key k}
           [mui/menu-item {:on-click v} k])
      options)]])

;This function here or a 'utils' ns?
(defn format-currency
  [value]
  (format/format "%.2f" value))
(defn asset-item [{:keys [title value on-click icon indent no-divider]}]
  [mui/box
   [mui/card-action-area {:on-click on-click
                          :sx {:padding-top 1 :padding-bottom 1}}
    [mui/stack (merge
                 {:direction :row
                  :spacing 2
                  :justify-content :space-between}
                 (when indent
                   {:sx {:pl (* indent 2)}}))
     (if (string? icon)
       [mui/box {:component :img
                 :src icon
                 :sx {:width 25 :mr 1}}]
       icon)
     [mui/typography {:variant :body1
                      :noWrap true
                      :sx {:width "100%"}}
      title]
     (when (number? value)
       [mui/typography {:variant :body1
                        :sx {:font-weight :bold}}
        (str "£" (format-currency value))])]]
   (when-not no-divider
     [mui/divider])])

;Tasks
(def task-icon "/images/green.png")

(defn task-item [{:keys [title body icon-path on-click href role]
                  :or {role :personal-representative
                       icon-path task-icon}}]
  [:<>
   [mui/list-item-button (merge {:on-click on-click
                                 :href href
                                 :disableGutters true}
                                (when (or on-click href)
                                  {:sx {:cursor :pointer}}))
    [mui/list-item-icon {:sx {:min-width "46px"}}
     (case role
       :personal-representative [:img {:src icon-path :width "30px" :height "30px"}]
       :laywer [ui/icon-admin-panel-settings-outlined {:fontSize "large"}])]
    [mui/list-item-text {:primary title
                         :secondary body}]]
   [mui/divider]])