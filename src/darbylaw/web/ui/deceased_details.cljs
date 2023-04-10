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
    [mui/box
     (cond
       (= ".pdf" (<< ::keydocs-model/death-certificate-extension))
       [pdf-viewer {:src src
                    :sx {:height "80vh"}}]
       :else
       #_[mui/stack
          [mui/typography {:variant :overline}
           "death certificate"]]
       [mui/box {:component :img
                 :src src
                 :sx {:max-width 1
                      :max-height 1
                      :display :block}}])]))

(defn panel []
  (r/with-let [case-loaded? (case-model/await-load-case!)]
    [mui/stack {:direction :row
                :sx {:height 1
                     :width 1
                     :overflow :auto}}
     [certificate-preview]
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

