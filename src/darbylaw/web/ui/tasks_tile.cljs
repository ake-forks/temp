(ns darbylaw.web.ui.tasks-tile
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [vlad.core :as v]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.deceased-details-form :as dd-form]
    [darbylaw.web.ui.banking.model :as banking-model]))

(defn task-item [{:keys [title body icon-path on-click href]}]
  [:<>
   [mui/card-action-area {:on-click on-click :href href}
    [mui/stack {:direction :row
                :spacing 1
                :style {:margin-top "0.25rem"
                        :margin-bottom "0.5rem"}}
     [mui/box {:style {:align-self :center :margin "0.5rem"}}
      [:img {:src icon-path :width "30px" :height "30px"}]]
     [mui/stack
      [mui/typography {:variant :h6 :font-weight 600} title]
      [mui/typography {:variant :body1} body]]]]
   [mui/divider]])

(def task-icon "/images/green.png")

(defn stage->copy-text
  [stage]
  (case stage
    :edit "please finish adding accounts"
    :notify "please approve the notification letter"
    :valuation "please finish adding valuations"))

(defn banking-task [type {:keys [bank-id] :as asset-data}]
  (let [stage (banking-model/get-asset-stage asset-data)
        id (get asset-data (case type
                             :bank :bank-id
                             :buildsoc :buildsoc-id))]
    (when-not (= stage :complete)
      [task-item
       {:title (str "tasks for "
                    (banking-model/asset-label type id))
        :body (stage->copy-text stage)
        :icon-path task-icon
        :on-click #(rf/dispatch [::banking-model/show-process-dialog type id])}])))

(defn valid? [validations data]
  (->> data
       (v/validate validations)
       (remove #(= (:type %) :darbylaw.web.util.vlad/valid-dayjs-date))
       empty?))

(defn case-tasks [{case-id :id :keys [deceased personal-representative]}]
  [:<>
   (when (not (valid? dd-form/data-validation deceased))
     [task-item
      {:title (str "missing deceased details")
       :body "please add deceased details"
       :icon-path task-icon
       :href (kf/path-for [:deceased-details {:case-id case-id}])}])])

(defn tasks-tile []
  [mui/card {:style {:height "350px" :background-color theme/off-white}}
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:mb 1}}
     "tasks"]
    [mui/divider]

    [mui/stack {:sx {:overflow-y :auto :max-height 280}}
     [case-tasks @(rf/subscribe [::case-model/current-case])]
     (for [bank @(rf/subscribe [::banking-model/banks])]
       ^{:key (:bank-id bank)}
       [banking-task :bank bank])
     (for [buildsoc @(rf/subscribe [::banking-model/building-societies])]
       ^{:key (:buildsoc-id buildsoc)}
       [banking-task :buildsoc buildsoc])]]])
