(ns darbylaw.web.ui.notification.dialog
  (:require [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.ui.case-model :as case-model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.notification.model :as model]))

(rf/reg-event-db ::open
  (fn [db [_ notification]]
    (-> db
      (model/set-current-notification notification)
      (assoc-in [::context :dialog-open?] true))))

(rf/reg-event-db ::close
  (fn [db _]
    (assoc-in db [::context :dialog-open?] false)))

(rf/reg-sub ::context #(::context %))

(rf/reg-sub ::dialog-open?
  :<- [::context]
  #(:dialog-open? %))

(defn asset-data [] [:i "[asset data goes here]"])

(defn letter-viewer [] "letter-viewer")
(defn conversation [] "conversation")

(defn left-panel [] "left-panel")

(rf/reg-event-db ::set-data-completed
  (fn [db [_ completed?]]
    (assoc-in db [::context :data-completed?] completed?)))

(rf/reg-sub ::data-completed?
  :<- [::context]
  #(get % :data-completed? false))

(defn right-panel []
  [:<>
   [mui/dialog-title
    (case (<< ::model/notification-type)
      :utility (str "household bills for " (<< ::model/utility-company-label)))
    [mui/icon-button {:onClick #(rf/dispatch [::close])}
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
                     " and ask for confirmation of the data entered."))]
       [mui/form-control-label
        {:label "All data is completed."
         :control
         (r/as-element
           [mui/checkbox {:checked (<< ::data-completed?)
                          :onChange #(rf/dispatch [::set-data-completed (ui/event-target-checked %)])}])}]])]
   [mui/dialog-actions
    [mui/fade {:in (<< ::data-completed?)}
     [mui/button {:variant :contained
                  :onClick (let [case-id (<< ::case-model/case-id)
                                 context (<< ::model/notification)]
                             #(rf/dispatch [::model/start-notification-process case-id context]))
                  :sx {:visibility (if (<< ::data-completed?) :visible :hidden)}}
      "Notify company"]]
    [mui/button {:variant :outlined
                 :onClick #(rf/dispatch [::close])}
     "Close"]]])

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
  [mui/dialog {:open (boolean (<< ::dialog-open?))
               :maxWidth (if (<< ::model/notification-ongoing?) :xl :sm)
               :fullWidth true}
   [dialog-content]])
