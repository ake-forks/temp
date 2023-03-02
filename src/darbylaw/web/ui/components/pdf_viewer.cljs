(ns darbylaw.web.ui.components.pdf-viewer
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]))

(defn pdf-viewer [{:keys [src sx]}]
  (r/with-let [loading? (r/atom true)]
    [:<>
     [mui/box {:sx (merge {:mt 2
                           :text-align :center}
                          (when-not @loading?
                            {:display :none})
                          sx)}
      [mui/circular-progress]]
     [mui/box {:src src
               :component :iframe
               :frameBorder 0
               :onLoad #(reset! loading? false)
               :sx (merge (when @loading?
                            {:display :none})
                          sx)}]]))
