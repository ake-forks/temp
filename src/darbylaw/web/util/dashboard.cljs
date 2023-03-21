(ns darbylaw.web.util.dashboard
  (:require
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [reagent.format :as format]
    [darbylaw.web.ui :as ui]))

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
