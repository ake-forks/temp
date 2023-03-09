(ns darbylaw.web.ui.case-commons
  (:require [reagent-mui.components :as mui]))

(defn fake-case-chip [fake?]
  [mui/chip (merge {:variant :outlined}
                   (if fake?
                     {:label "FAKE"
                      :color :secondary}
                     {:label "REAL"
                      :color :primary}))])
