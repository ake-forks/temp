(ns darbylaw.workspaces.mui-timeline-cards
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [darbylaw.web.mui-components :as mui]
            [darbylaw.web.mui.timeline-components :as tl]
            [darbylaw.web.mui.stepper-components :as stepper]
            [darbylaw.workspaces.workspace-styles :as style]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]))

(def green [:img {:src "/images/green.png" :width 30 :height 30}])
(def orange [:img {:src "/images/orange.png" :width 30 :height 30}])

(defn progress-bar []
  [stepper/stepper {:alternativeLabel true :className (style/mui-default)}
   [stepper/step
    [stepper/label {:icon (r/as-element green)} "Upload Will"]
    ]
   [stepper/step
    [stepper/label {:icon (r/as-element green)} "Add Banks"]
    ]
   [stepper/step
    [stepper/label {:icon (r/as-element orange)} "Notify Institutions"]
    ]
   [stepper/step
    [stepper/label {:icon (r/as-element orange)} "Close Utility Accounts"]
    ]
   ]
  )



(ws/defcard mui-timeline-card
  (ct.react/react-card
    (r/as-element [progress-bar])))
