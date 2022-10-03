(ns darbylaw.workspaces.antd-timeline-cards
  (:require ["antd" :as antd]
            [reagent.core :as r]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]))

(def green [:img {:src "/images/green.png" :width 30 :height 30}])
(def orange [:img {:src "/images/orange.png" :width 30 :height 30}])


(defn timeline []
  [:> antd/Steps {:labelPlacement :vertical}
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "finish" :title "Upload Will" :icon (r/as-element green)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "finish" :title "Add Banks" :icon (r/as-element green)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "process" :title "Notify Institutions" :icon (r/as-element orange)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "wait" :title "Close Utility Accounts" :icon (r/as-element orange)}]
   ]
  )




;; -- Entry Point -------------------------------------------------------------

; let's make a card out of it!
(ws/defcard antd-timeline-card
  (ct.react/react-card
    (r/as-element [timeline])))
