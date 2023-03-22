(ns darbylaw.web.ui.deceased-details
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [darbylaw.web.ui :refer [<<]]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.deceased-details-form :as form]
            [darbylaw.web.ui.components.pdf-viewer :refer [pdf-viewer]]))

(defn certificate-preview []
  (let [case-id (<< ::case-model/case-id)]
    [mui/box {:sx {:flex-grow 1}}
     [mui/stack
      [mui/typography {:variant :overline}
       "death certificate"]
      [pdf-viewer {:src (str "/api/case/" case-id "/document/death-certificate")
                   :sx {:height "80vh"}}]]]))

(defn panel []
  (r/with-let [case-loaded? (case-model/await-load-case!)]
    [mui/stack {:direction :row}
     [mui/container {:max-width :sm}
      [mui/typography {:variant :h4
                       :sx {:pt 4 :pb 2}}
       "deceased's details"]
      (if @case-loaded?
        [form/deceased-details-form :edit
         {:initial-values (:deceased @(rf/subscribe [::case-model/current-case]))}]
        [mui/circular-progress])]
     [certificate-preview]]))
