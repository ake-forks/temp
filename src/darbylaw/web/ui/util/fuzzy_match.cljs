(ns darbylaw.web.ui.util.fuzzy-match
  (:require ["fuzzball" :as fuzz]))

(defn ratio [s1 s2]
  (.ratio fuzz s1 s2))

(defn partial-ratio [s1 s2]
  (.partial_ratio fuzz s1 s2))
