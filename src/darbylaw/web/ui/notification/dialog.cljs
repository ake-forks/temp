(ns darbylaw.web.ui.notification.dialog
  (:require [darbylaw.web.ui :as ui :refer [<<]]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.notification.model :as model]
            [darbylaw.web.ui.bills.model :as bills-model]
            [darbylaw.web.ui.notification.conversation :as conversation]
            [darbylaw.web.ui.bills.account-info :as account-info]
            [darbylaw.api.bill.data :as bill-data]
            [darbylaw.web.ui.pensions.model :as pensions-model]
            [darbylaw.web.ui.pensions.shared :as pensions-shared]
            [darbylaw.web.ui.components.dialog :as dialog]
            [darbylaw.web.ui.bills.common :as bills-common]))

(defn asset-data []
  (case (<< ::model/notification-type)
    :utility [account-info/utility-info]
    :council-tax [account-info/council-tax-info]
    :pension [pensions-shared/account-info (:provider (<< ::model/notification))]
    "data default"))

(defn at-address []
  [mui/stack {:align-items :flex-start}
   [mui/typography "at address:"]
   (bills-common/address-box false
     [mui/typography (<< ::bills-model/address)])])

(defn get-data []
  (let [notification-db (<< ::model/notification)]
    (first (case (<< ::model/notification-type)
             :utility (bills-model/current-utility-data
                        (:utility-company notification-db)
                        (:property notification-db))
             :council-tax (bills-model/current-council-data
                            (:utility-company notification-db)
                            (:property notification-db))
             :pension (pensions-model/get-pension (:provider notification-db))))))

(defn get-label [current-data]
  (let [notification-db (<< ::model/notification)]
    (case (<< ::model/notification-type)
      :council-tax (bill-data/get-council-label (:council notification-db))
      :utility (if (some? (:new-utility-name current-data))
                 (:new-utility-name current-data)
                 (<< ::bills-model/utility-company-label))
      :pension
      (pensions-model/get-label (:provider notification-db))
      "label default")))

(defn get-copy [label]
  (let [notification-type (<< ::model/notification-type)]
    (case notification-type
      :utility (str
                 "Let us know when you have provided all accounts at this address"
                 " for " label ". At that point,"
                 " we will notify the company about the decease"
                 " and ask for confirmation of the data entered.")
      :council-tax (str
                     "Once you are happy that the information provided is correct,"
                     " we will notify " label ".")
      :pension (str
                 "Once you are happy that the information provided is correct,"
                 " we will notify " label ".")
      "copy default")))

(defn right-panel []
  (let [current-data (get-data)
        label (get-label current-data)]
    [mui/stack
     [dialog/title {:on-click-close #(do (reset! pensions-model/edit-mode false) (rf/dispatch [::model/close-dialog]))}
      (case (<< ::model/notification-type)
        :utility
        [mui/stack
         label
         [at-address]]
        :pension
        [mui/stack label]
       "default")]
     [mui/dialog-content {:sx {:pt 0
                               :width 540}}
      [asset-data]
      (when-not (<< ::model/notification-ongoing?)
        [mui/stack {:sx {:mt 2}}
         [mui/typography {:sx {:font-weight 600}}
          "Finished?"]
         [mui/typography
          (get-copy label)]
         [mui/form-control-label
          {:label "All data is completed."
           :control
           (r/as-element
             [mui/checkbox {:checked (<< ::model/data-completed?)
                            :onChange #(rf/dispatch [::model/set-data-completed (ui/event-target-checked %)])}])}]])]
     [mui/dialog-actions
      [mui/fade {:in (and (not (<< ::model/notification-ongoing?))
                          (<< ::model/data-completed?))}
       [mui/button {:variant :contained
                    :onClick (let [notification (<< ::model/notification)]
                               #(rf/dispatch [::model/start-notification notification]))
                    :sx {:visibility (if (<< ::model/data-completed?) :visible :hidden)}}
        (case (<< ::model/notification-type)
          :utility "Notify company"
          :council-tax "Notify council"
          :pension "Notify Provider")]]
      [mui/button {:variant :outlined
                   :onClick #(rf/dispatch [::model/close-dialog])}
       "Close"]]]))

(defn dialog-content []
  [mui/stack {:spacing 1
              :direction :row
              :sx {:height "100%"}}
   ; This was a try to introduce animation when the conversation panel is first shown.
   ; Layout was too challenging to setup correctly.
   #_[mui/collapse (-> {:in (<< ::model/notification-ongoing?)
                        :orientation :horizontal
                        :spacing 1
                        :sx {:flex-grow 1}}
                     (ui/make-collapse-contents-full-width))]
   (when (<< ::model/notification-ongoing?)
     [mui/box {:sx {:flex-grow 1}}
      [conversation/panel]])
   [right-panel]])

(defn dialog []
  [mui/dialog {:open (boolean (<< ::model/dialog-open?))
               :maxWidth (if (<< ::model/notification-ongoing?) :xl :sm)
               :fullWidth (<< ::model/notification-ongoing?)
               :PaperProps {; full height
                            :sx {:height "100%"}}}
   [dialog-content]])