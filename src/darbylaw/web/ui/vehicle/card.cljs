(ns darbylaw.web.ui.vehicle.card
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.util.dashboard :refer [asset-add-button asset-card asset-item]]
    [darbylaw.web.ui.vehicle.model :as model]))

(defn card []
  (let [vehicles @(rf/subscribe [::model/vehicles])]
    [asset-card {:title "Vehicles"}
     (for [{:keys [vehicle-id registration-number
                   estimated-value confirmed-value]}
           vehicles]
       ^{:key vehicle-id}
       [asset-item
        {:title registration-number
         :on-click #(rf/dispatch [::model/select-vehicle vehicle-id])}])
         :value (js/parseFloat (or confirmed-value estimated-value "0"))
     [asset-add-button
      {:title "add"
       :on-click #(rf/dispatch [::something])}]]))
