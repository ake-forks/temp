(ns darbylaw.web.ui.overview-tile
  (:require
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.banking.model :as bank-model]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [clojure.string :as str]))

(defn parse-float
  [s]
  (if (str/blank? s)
    0
    (js/parseFloat s)))

(defn get-value []
  (let [bank-accounts @(rf/subscribe [::bank-model/banks])
        buildsoc-accounts @(rf/subscribe [::bank-model/building-societies])
        funeral-account @(rf/subscribe [::funeral-model/account])
        funeral-expenses @(rf/subscribe [::funeral-model/expense-list])
        
        banking (->> (concat bank-accounts buildsoc-accounts)
                     (mapcat :accounts)
                     (map #(if-let [confirmed-value (:confirmed-value %)]
                             confirmed-value
                             (:estimated-value %)))
                     (map parse-float))
        funeral (concat [(-> funeral-account :value parse-float)]
                        (->> funeral-expenses
                             (map :value)
                             (map parse-float)))
        
        assets (concat banking funeral)]
    (reduce + assets)))

(defn overview-card []
  [mui/card {:style {:width "large" :background-color theme/off-white}}
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:mb 1}}
     "estimated value"]
    [mui/divider]
    [mui/typography {:variant :h4 :sx {:mt 1}}
     "£" (format/format "%.2f" (get-value))]]])


