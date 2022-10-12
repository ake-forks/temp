(ns darbylaw.web.ui.bank
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]))

(def add-bank-visible (r/atom false))

(defn add-bank-toggle []
  (swap! add-bank-visible true))


(defn add-bank-modal []
  [mui/modal {:open @(add-bank-visible)}
   [mui/typography "add a bank"]])


