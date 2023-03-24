(ns darbylaw.web.ui.properties.dashboard
  (:require
    [darbylaw.api.util.data :as data-util]
    [darbylaw.web.ui.app-layout :as l]
    [darbylaw.web.ui.properties.dialog :as dialog]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.properties.model :as model]
    [re-frame.core :as rf]
    [darbylaw.web.ui :refer (<<)]))

(defn properties-card []
  (let [properties (filter #(true? (:owned? %)) (<< ::case-model/properties))]
    [:<>
     [dialog/dialog]
     [l/asset-card {:title "properties"}
      (for [{:keys [address id]} properties]
        ^{:key id}
        [l/asset-item
         {:title (data-util/first-line address)
          :on-click #(rf/dispatch [::model/show-dialog id :edit])}])
      [l/asset-add-button
       {:title "add"
        :on-click #(rf/dispatch [::model/show-dialog nil :add])}]]]))
