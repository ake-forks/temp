(ns darbylaw.web.ui.tasks-tile
  (:require
    [darbylaw.web.ui :as ui]
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [kee-frame.core :as kf]
    [vlad.core :as v]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.deceased-details-form :as dd-form]
    [darbylaw.web.ui.banking.tasks :as banking-tasks]
    [darbylaw.web.ui.keydocs.tasks :refer [keydocs-tasks]]
    [darbylaw.web.ui.identity.tasks :refer [identity-tasks]]
    [darbylaw.web.ui.vehicle.tasks :refer [vehicle-tasks]]))

(defn valid? [validations data]
  (->> data
    (v/validate validations)
    (remove #(= (:type %) :darbylaw.web.util.vlad/valid-dayjs-date))
    empty?))

(defn case-tasks [{case-id :id :keys [deceased personal-representative]}]
  [:<>
   (when (not (valid? dd-form/data-validation deceased))
     [layout/task-item
      {:title (str "missing deceased details")
       :body "please add deceased details"
       :icon-path layout/task-icon
       :href (kf/path-for [:deceased-details {:case-id case-id}])}])])

(defn tasks-tile []
  [mui/card {:style {:height "350px" :background-color theme/off-white}}
   [mui/card-content {:sx {:paddingTop (ui/theme-spacing 1.5)}}
    [mui/typography {:variant :h5 :sx {:mb 1}}
     "tasks"]
    [mui/divider]
    [mui/stack {:sx {:overflow-y :auto :max-height 280}}
     [identity-tasks]
     [case-tasks @(rf/subscribe [::case-model/current-case])]
     [keydocs-tasks]
     [banking-tasks/banks]
     [banking-tasks/buildsocs]
     [vehicle-tasks]]]])


