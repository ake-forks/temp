(ns darbylaw.web.ui.banking.tasks
  (:require
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.app-layout :as layout]
    [re-frame.core :as rf]
    [reagent.core :as r]))


(defn stage->copy-text
  [stage]
  (case stage
    :edit "please finish adding accounts"
    :notify "please approve the notification letter"
    :valuation "please finish adding valuations"))
(defn task [type {:keys [bank-id] :as asset-data}]
  (let [stage (model/get-asset-stage asset-data)
        id (get asset-data (case type
                             :bank :bank-id
                             :buildsoc :buildsoc-id))]
    (when-not (= stage :complete)
      [layout/task-item
       {:title (str "tasks for "
                 (model/asset-label type id))
        :body (stage->copy-text stage)
        :icon-path layout/task-icon
        :on-click #(rf/dispatch [::model/show-process-dialog type id])}])))

(defn banks []
  (for [bank @(rf/subscribe [::model/banks])]
    ^{:key (:bank-id bank)}
    (r/as-element [task :bank bank])))

(defn buildsocs []
  (for [buildsoc @(rf/subscribe [::model/building-societies])]
    ^{:key (:buildsoc-id buildsoc)}
    (r/as-element [task :buildsoc buildsoc])))
