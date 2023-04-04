(ns darbylaw.web.ui.properties.shared
  (:require
    [re-frame.core :as rf]
    [darbylaw.web.ui :refer (<<)]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.properties.model :as model]
    [reagent-mui.components :as mui]))

(defn confirmation-popover []
  (let [case-id (<< ::case-model/case-id)
        popover model/popover]
    [mui/popover {:open (contains? @popover :label)
                  :anchor-el (get @popover :anchor)
                  :on-close #(reset! popover nil)
                  :anchor-origin {:vertical "bottom" :horizontal "right"}
                  :transform-origin {:vertical "top" :horizontal "right"}}
     [mui/stack {:sx {:p 1.5}}
      [mui/typography "Are you sure?"]
      [mui/button {:variant :text
                   :color :error
                   :full-width true
                   :on-click #(case (get @popover :label)
                                "document" (rf/dispatch [::model/remove-file case-id
                                                         (get @popover :property-id)
                                                         (get @popover :filename)])
                                "property" (rf/dispatch [::model/remove-property
                                                         case-id
                                                         (get @popover :property-id)]))}
       "yes, remove " (get @popover :label)]]]))

(defn cant-delete-popover []
  (let [case-id (<< ::case-model/case-id)
        popover model/popover]
    [mui/popover {:open (contains? @popover :required-by)
                  :anchor-el (get @popover :anchor)
                  :on-close #(reset! popover nil)
                  :anchor-origin {:vertical "bottom" :horizontal "right"}
                  :transform-origin {:vertical "top" :horizontal "right"}}
     [mui/stack {:sx {:p 1.5}}
      [mui/typography "Remove as an owned property? Currently in use for "]
      [mui/typography (:required-by @popover)]
      [mui/button {:variant :text
                   :color :error
                   :full-width true
                   :on-click
                   #(rf/dispatch [::model/remove-owned case-id (get @popover :property-id)])}
       "yes, remove"]]]))
