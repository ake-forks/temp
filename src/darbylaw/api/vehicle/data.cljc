(ns darbylaw.api.vehicle.data
  (:require [darbylaw.api.util.malli :as malli+]))

(def schema
  [:and
   [:map
    [:registration-number :string]
    [:description {:optional true} :string]
    [:estimated-value {:optional true} :string]
    [:sold {:optional true} :boolean]
    [:sold-by {:optional true} :string]
    [:confirmed-value {:optional true} :string]]
   (malli+/when-match [:map [:sold true?]]
     (malli+/required [:sold-by :confirmed-value]))])

(def props
  (->> schema second rest (map first)))
