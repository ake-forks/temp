(ns darbylaw.web.ui.overview-tile
  (:require
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.case-model :as case-model]))

(defn get-value []
  (let [bank-accounts (:bank-accounts @(rf/subscribe [::case-model/current-case]))]
    (reduce + (map
                (fn [bank]
                  (reduce + (map (fn [account]
                                   (if (clojure.string/blank? (:estimated-value account))
                                     0
                                     (js/parseFloat (:estimated-value account))))
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


