(ns darbylaw.workspaces.diff-cards
  (:require [nubank.workspaces.core :refer [defcard]]
            [nubank.workspaces.card-types.react :refer [react-card]]
            [reagent.core :as r]
            [darbylaw.web.ui.diff :refer [diff]]))

(defcard render-sequence-diffs
  (react-card
    (r/as-element
      [:div {:style {:display :flex
                     :flex-direction :column}}
       (diff {:a 1} {:a 2})
       [:hr {:width "100%"}]
       (diff {:a 1} {:a [2]})
       [:hr {:width "100%"}]
       (diff {:a [1 4 5]} {:a [2 3]})
       [:hr {:width "100%"}]
       (diff
         {:a [{:a 1 :b [{:c 1}]} {:a 2 :b [{:d 2}]}]}
         {:a [{:a 1 :b []}       {:a 2 :b [{:d 4}]}]})])))
