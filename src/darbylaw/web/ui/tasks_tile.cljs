(ns darbylaw.web.ui.tasks-tile
  (:require
    [darbylaw.web.ui :as ui :refer [<<]]
    [reagent-mui.components :as mui]
    [kee-frame.core :as kf]
    [vlad.core :as v]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.app-layout :as layout]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.deceased-details-form :as dd-form]
    [darbylaw.web.ui.banking.tasks :as banking-tasks]
    [darbylaw.web.ui.keydocs.tasks :as keydocs :refer [keydocs-tasks]]
    [darbylaw.web.ui.identity.tasks :refer [identity-tasks]]
    [darbylaw.web.ui.vehicle.tasks :refer [vehicle-tasks]]
    [darbylaw.web.ui.other.tasks :refer [other-asset-tasks]]))

(defn valid? [validations data]
  (->> data
    (v/validate validations)
    (remove #(= (:type %) :darbylaw.web.util.vlad/valid-dayjs-date))
    empty?))

(defn case-tasks []
  (let [{case-id :id
         :keys [deceased]} (<< ::case-model/current-case)]
    (when (not (valid? dd-form/data-validation deceased))
      [layout/task-item
       {:title "complete deceased details"
        :role :laywer
        :href (kf/path-for [:deceased-details {:case-id case-id}])}])))

(defn tasks-tile []
  [mui/card {:style {:height "350px" :background-color theme/off-white}}
   [mui/card-content {:sx {:paddingTop (ui/theme-spacing 1.5)}}
    [mui/typography {:variant :h5 :sx {:mb 1}}
     "tasks"]
    [mui/divider]
    [mui/stack {:sx {:overflow-y :auto :max-height 280}}
     [keydocs/death-certificate-task]
     [case-tasks]
     [identity-tasks]
     [keydocs-tasks]
     [banking-tasks/banks]
     [banking-tasks/buildsocs]
     [vehicle-tasks]
     [other-asset-tasks]]]])