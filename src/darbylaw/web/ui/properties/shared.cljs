(ns darbylaw.web.ui.properties.shared
  (:require
    [darbylaw.api.util.data :as data-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :refer (<<)]
    [darbylaw.web.ui.app-layout :as l]
    [darbylaw.web.ui.case-model :as case-model]))

(defn properties-card []
  (let [properties (<< ::case-model/properties)]
    [l/asset-card {:title "properties"}
     (for [{:keys [address id]} properties]
       ^{:key id}
       [l/asset-item
        {:title (data-util/first-line address)
         :on-click #(print id)}])
     [l/asset-add-button
      {:title "add"
       :on-click #(print "add property")}]]))