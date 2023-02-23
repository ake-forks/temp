(ns darbylaw.web.ui.notification.dialog
  (:require [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.ui.case-model :as case-model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.notification.model :as model]
            [darbylaw.web.ui.bills.account-info :as account-info]
            [darbylaw.api.bill.data :as bill-data]))

(defn asset-data []
  (case (<< ::model/notification-type)
    :utility [account-info/utility-bill-info]
    :council-tax [account-info/council-tax-info]))

(defn letter-viewer [] "letter-viewer")
(defn conversation [] "conversation")

(defn left-panel [] "left-panel")

(defn right-panel []
  (let [council-label (bill-data/get-council-label (:council (<< ::model/notification)))]
    [:<>
     [mui/dialog-title
      (case (<< ::model/notification-type)
        :utility (str "household bills for " (<< ::model/utility-company-label))
        :council-tax council-label)
      [mui/icon-button {:onClick #(rf/dispatch [::model/close-dialog])}
       [ui/icon-close]]]
     [mui/dialog-content
      (when-not (<< ::model/notification-ongoing?)
        [mui/stack
         [asset-data]
         [mui/typography {:sx {:font-weight 600}}
          "Finished?"]
         [mui/typography
          (case (<< ::model/notification-type)
            :utility (str
                       "Let us know when you have provided all accounts at this address"
                       " for " (<< ::model/utility-company-label) ". At that point,"
                       " we will notify the company about the decease"
                       " and ask for confirmation of the data entered.")
            :council-tax (str
                           "Once you are happy that the information provided is correct,"
                           " we will notify " council-label "."))]
         [mui/form-control-label
          {:label "All data is completed."
           :control
           (r/as-element
             [mui/checkbox {:checked (<< ::model/data-completed?)
                            :onChange #(rf/dispatch [::model/set-data-completed (ui/event-target-checked %)])}])}]])]
     [mui/dialog-actions
      [mui/fade {:in (<< ::model/data-completed?)}
       [mui/button {:variant :contained
                    :onClick (let [case-id (<< ::case-model/case-id)
                                   context (<< ::model/notification)]
                               #(rf/dispatch [::model/start-notification-process case-id context]))
                    :sx {:visibility (if (<< ::model/data-completed?) :visible :hidden)}}
        "Notify company"]]
      [mui/button {:variant :outlined
                   :onClick #(rf/dispatch [::model/close-dialog])}
       "Close"]]]))

(defn dialog-content []
  [mui/stack {:spacing 1
              :direction :row
              :sx {:height "95vh"}}
   [mui/collapse (-> {:in (<< ::model/notification-ongoing?)
                      :orientation :horizontal
                      :spacing 1
                      :sx {:flex-grow 1}}
                   (ui/make-collapse-contents-full-width))
    [left-panel]]
   [mui/stack {:sx {:width 620}}
    [right-panel]]])

(defn dialog []
  [mui/dialog {:open (boolean (<< ::model/dialog-open?))
               :maxWidth (if (<< ::model/notification-ongoing?) :xl :sm)
               :fullWidth true}
   [dialog-content]])