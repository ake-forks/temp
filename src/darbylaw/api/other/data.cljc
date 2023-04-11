(ns darbylaw.api.other.data
  (:require [darbylaw.api.util.malli :as malli+]))

(def schema
  [:and
   [:map
    [:name :string]
    [:note {:optional true} :string]
    [:value {:optional true} :string]
    [:paid {:optional true} :boolean]
    [:paid-at {:optional true} :string]]
   (malli+/when-match [:map [:paid true?]]
     (malli+/required [:paid-at]))])

(def props
  (->> schema second rest (map first)))
