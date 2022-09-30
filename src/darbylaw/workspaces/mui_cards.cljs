(ns darbylaw.workspaces.mui-cards
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [darbylaw.web.mui-components :as mui]
            [darbylaw.workspaces.workspace-styles :as style]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [reagent-mui.icons.account-balance :as bankicon]
            [reagent-mui.icons.add-circle :as addicon]))



(defn asset-item [name amount]
  [mui/stack {:sx {:margin-top "0.3rem" :margin-bottom "0.2rem"} :direction :row :spacing 0.5 :justify-content :space-between :align-items :center}
   [bankicon/account-balance]
   [mui/typography {:variant :h5} name]
   [mui/typography {:variant :h5} "£" amount]
   ])

(defn add-account []

  [mui/stack {:sx {:padding "0.2rem"} :direction :row :spacing 0.5 :justify-content :flex-start :align-items :center}

   [mui/typography {:variant :h5} "add account"]
   [addicon/add-circle]
   ])

(defn asset-card []
  [mui/card {:className (style/mui-default)}
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:font-weight :bold}} "bank accounts"]
    [mui/divider {:variant :middle}]
    [asset-item "Barclays" 5000]
    [mui/divider {:variant :middle}]
    [asset-item "Santander" 3500]
    [mui/divider {:variant :middle}]
    ]
   [mui/card-action-area
    [add-account]]
   ]
  )


; let's make a card out of it!
(ws/defcard mui-card
  (ct.react/react-card
    (reagent/as-element [asset-card])))