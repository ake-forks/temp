(ns darbylaw.web.ui.bills.common
  (:require [reagent-mui.components :as mui]))

(defn address-box [selected? child]
  [mui/paper (merge
               {:variant :outlined
                :sx (merge
                      {:flex-grow 1
                       :border-width 2
                       :padding 1
                       :white-space :pre}
                      (when selected?
                        {:border-color :primary.light}))})
   child])