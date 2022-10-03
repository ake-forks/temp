(ns darbylaw.workspaces.antd-cards
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            ["antd" :as antd]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]))

(defn asset-item [name amount]
  [antd/Space {:direction :vertical}
   [:h5 name]
   [:h5 amount]
   ]
  )

(defn ui
  []
  [:div
   [:> antd/Card {:title "bank accounts" :headStyle {:font-weight 600}}
    [:> antd/Divider]
    [asset-item "Santander" 5000]]]

  )

;; -- Entry Point -------------------------------------------------------------

; let's make a card out of it!
(ws/defcard antd-card
  (ct.react/react-card
    (reagent/as-element [ui])))