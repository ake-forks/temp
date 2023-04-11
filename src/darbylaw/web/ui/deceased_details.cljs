(ns darbylaw.web.ui.deceased-details
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui :refer [<<]]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.keydocs.model :as keydocs-model]
            [darbylaw.web.ui.deceased-details-form :as form]
            [darbylaw.web.ui.components.pdf-viewer :refer [pdf-viewer]]))

(defn certificate-preview []
  (let [case-id (<< ::case-model/case-id)
        src (str "/api/case/" case-id "/document/death-certificate")]
    (cond
      (= ".pdf" (<< ::keydocs-model/death-certificate-extension))
      [pdf-viewer {:src src
                   :sx {:height 1
                        :flex-grow 2}}]
      :else
      [mui/box {:m 1}
       [mui/box {:component :img
                 :src src
                 :sx {:max-width 1
                      :max-height 1
                      :display :block}}]])))

(rf/reg-event-fx ::go-to-keydocs
  (fn [_ [_ case-id]]
    {:dispatch [::keydocs-model/show-dialog]
     :navigate-to [:dashboard {:case-id case-id}]}))

(defn panel []
  (r/with-let [case-loaded? (case-model/await-load-case!)]
    [mui/stack {:direction :row
                :sx {:height 1
                     :width 1
                     :overflow :auto}}
     (if (<< ::keydocs-model/document-present? :death-certificate)
       [certificate-preview]
       [mui/box {:flex-grow 1
                 :sx {:text-align :center
                      :margin :auto}}
        [mui/typography "Death certificate not uploaded yet"]
        [mui/button {:onClick (let [case-id (<< ::case-model/case-id)]
                                #(rf/dispatch [::go-to-keydocs case-id]))}
         "go to key documents"]])
     [mui/box {:sx {:overflow :auto
                    :flex-grow 1
                    :flex-shrink 0
                    :pb 2}}
      [mui/container {:max-width :sm}
       [mui/typography {:variant :h4
                        :sx {:pt 4 :pb 2}}
        "deceased's details"]
       (if @case-loaded?
         [form/deceased-details-form :edit
          {:initial-values (:deceased @(rf/subscribe [::case-model/current-case]))}]
         [mui/circular-progress])]]]))

