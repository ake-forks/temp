(ns darbylaw.web.ui.case-commons
  (:require [reagent-mui.components :as mui]))

(defn fake-case-chip [fake?]
  [mui/chip {:label (if fake? "FAKE" "REAL")
             :variant :outlined}])
