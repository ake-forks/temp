(ns darbylaw.web.ui.overview-tile
  (:require
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(defn get-value []
  (let [bank-accounts (:bank-accounts @(rf/subscribe [::current-case]))]
    (reduce + (map
                (fn [bank]
                  (reduce + (map (fn [account] (js/parseFloat (:estimated-value account)))
                              (:accounts bank))))
                bank-accounts))))



(defn overview-card []
  [mui/card {:style {:width "large" :background-color theme/off-white}}
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:mb 1}}
     "estimated value"]
    [mui/divider]
    [mui/typography {:variant :h4 :sx {:mt 1}}
     "£" (format/format "%.2f" (get-value))]]])


