(ns darbylaw.web.ui.other.card
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.app-layout :refer [asset-add-button asset-card asset-item]]
    [darbylaw.web.ui.other.model :as model]
    [darbylaw.web.ui.other.dialog :as dialog]))

(defn card []
  (let [assets @(rf/subscribe [::model/assets])]
    [asset-card {:title "other"}
     (for [{:keys [asset-id name value]}
           assets]
       ^{:key asset-id}
       [asset-item
        {:title name
         :value (js/parseFloat (or value "0"))
         :on-click #(rf/dispatch [::model/set-dialog-open asset-id])}])
     [asset-add-button
      {:title "add"
       :on-click #(rf/dispatch [::model/set-dialog-open :add])}]
     [dialog/dialog]]))
