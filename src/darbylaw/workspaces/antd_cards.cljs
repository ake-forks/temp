(ns darbylaw.workspaces.antd-cards
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            ["antd" :as antd]
            ["@ant-design/icons" :as icon]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]))

(defn asset-item [name amount]
  [:> antd/Space {:direction :horizontal}
   [:> icon/BankOutlined {:style {:font-size :medium}}]
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} name]
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} "£" amount]
   ]
  )

(defn add-account []
  [:> antd/Space {:direction :horizontal}
   [:> antd/Button {:type :text}
    [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} "add account"]

    ]
   [:> icon/PlusCircleFilled]

   ]
  )

(defn ui []

  [:> antd/Card
   {
    :bodyStyle {:padding "0.5rem" :font-weight 600}}
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-weight 600 :font-size :medium :margin-bottom 0}} "bank accounts"]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [asset-item "Santander" 5000]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [asset-item "HSBC" 3500]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [add-account]
   ]

  )

;; -- Entry Point -------------------------------------------------------------

; let's make a card out of it!
(ws/defcard antd-card
  (ct.react/react-card
    (reagent/as-element [ui])))