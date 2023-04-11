(ns darbylaw.web.ui.other.card
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui.app-layout :refer [asset-add-button asset-card asset-item]]
    [darbylaw.web.ui.other.model :as model]
    [darbylaw.web.ui.other.dialog :as dialog]))

(defn card []
  (let [vehicles @(rf/subscribe [::model/vehicles])]
    [asset-card {:title "other"}
     (for [{:keys [vehicle-id registration-number
                   estimated-value confirmed-value]}
           vehicles]
       ^{:key vehicle-id}
       [asset-item
        {:title registration-number
         :value (js/parseFloat (or confirmed-value estimated-value "0"))
         :on-click #(rf/dispatch [::model/set-dialog-open vehicle-id])}])
     [asset-add-button
      {:title "add"
       :on-click #(rf/dispatch [::model/set-dialog-open :add])}]
     [dialog/dialog]]))
