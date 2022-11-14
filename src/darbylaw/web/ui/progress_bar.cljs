(ns darbylaw.web.ui.progress-bar
  (:require
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [re-frame.core :as rf]))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))

(def white [:img {:src "/images/white-step.png" :width "25px"}])
(def green [:img {:src "/images/green-step.png" :width "25px"}])
(def orange [:img {:src "/images/orange-step.png" :width "25px"}])
(def grey [:img {:src "/images/grey-step.png" :width "25px"}])
(def loading [:img {:src "/images/loading-step.png" :width "25px"}])

(defn get-icon [stage]
  (case stage
    0 (r/as-element grey)
    1 (r/as-element white)
    2 (r/as-element orange)
    3 (r/as-element loading)
    4 (r/as-element green)))

(defn progress-bar []
  (let [current-case @(rf/subscribe [::current-case])
        assets (if (some? (:bank-accounts current-case)) 2 1)
        notify (if (= assets 1) 0 1)
        valuations (case notify 0 0 1 0 2 2 3 2 4 2)
        grant (if (= 4 assets notify valuations) 2 0)]
    [mui/card
     [mui/stepper {:alternative-label true :non-linear true
                   :style {:margin-top "2rem" :margin-bottom "2rem"}}
      [mui/step {:completed true}
       [mui/step-label {:icon (get-icon 4)} "Case Created"]]
      [mui/tooltip {:title "We are waiting for SmartSearch to complete your ID check." :position "top"}
       [mui/step {:completed true}
        [mui/step-label {:icon (get-icon 3)} "Identity Check"]]]
      [mui/tooltip {:title "Add bank and utility assets via the dashboard." :position "top"}
       [mui/step {:completed (if (> assets 0) true false)}
        [mui/step-label {:icon (get-icon assets)} "Complete Assets"]]]
      [mui/tooltip {:title "We will trigger notification when one or more assets are complete." :position "top"}
       [mui/step {:completed (if (> notify 0) true false)}
        [mui/step-label {:icon (get-icon notify)} "Notify Institutions"]]]
      [mui/tooltip {:title "Not available until one or more institutions have been notified." :position "top"}
       [mui/step {:completed (if (> valuations 0) true false)}
        [mui/step-label {:icon (get-icon valuations)} "Receive Valuations"]]]
      [mui/tooltip {:title "Not available until all valuations have been received." :position "top"}
       [mui/step {:completed (if (> grant 0) true false)}
        [mui/step-label {:icon (get-icon grant)} "Grant of Probate"]]]]]))