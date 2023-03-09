(ns darbylaw.web.ui.progress-bar
  (:require
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.identity.dialog :as identity-dialog]
    [darbylaw.web.ui.identity.model :as identity-model]))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))

(defn get-icon [status]
  (case status
    ;; The step isn't available for the user
    ;; Grey so the user doesn't worry about it
    :unavailable
    [:img {:src "/images/grey-mui-leaf.png" :width "30px"}]
    ;; The user has tasks to do, or needs to kick of the step?
    ;; NOTE: Is this one needed?
    :available
    [:img {:src "/images/black-mui-leaf.png" :width "30px"}]
    ;; The user has tasks to do
    :tasks-available
    [:img {:src "/images/orange-mui-leaf.png" :width "30px"}]
    ;; Waiting on a background process to complete
    :waiting-on-us
    [:img {:src "/images/loading-step.png" :width "25px"}]
    ;; Step complete, no further action required
    :completed
    [:img {:src "/images/green-mui-leaf.png" :width "30px"}]))

(defn stepper-item [{:keys [label tooltip icon]}]
  [mui/step {}
   [mui/tooltip {:title (or tooltip "")}
    [mui/step-label (when icon
                      {:icon (r/as-element icon)})
     label]]])

(defn progress-bar []
  (let [current-case @(rf/subscribe [::current-case])]
    [mui/card
     [mui/stepper {:alternative-label true
                   :non-linear true
                   :style {:margin-top "2rem"
                           :margin-bottom "2rem"}}
      [stepper-item
       {:label "Case Created"
        :icon (get-icon :completed)}]
      [stepper-item 
       {:label "Identity Check"
        :tooltip (case @(rf/subscribe [::identity-model/current-final-result])
                   :unknown "We're waiting on an admin to run the checks."
                   :pass "Identity checks have passed."
                   "Some manual intervention is required.")
        :icon [identity-dialog/check-icon]}]
      [stepper-item 
       {:label "Complete Assets"
        :tooltip "Add bank and utitity assets via the dashboard."
        :icon (get-icon
                (if-not (some? (:bank-accounts current-case))
                 :available
                 :tasks-available))}]
      [stepper-item 
       {:label "Notify Institutions"
        :tooltip "We will trigger a notification when one or more assets are complete."
        :icon (get-icon
                (if (some? (:bank-accounts current-case))
                  :available
                  :unavailable))}]
      [stepper-item 
       {:label "Receive Valuations"
        :tooltip "Not available until one or more institutions have been notified."
        :icon (get-icon :unavailable)}]
      [stepper-item 
       {:label "Grant of Probate"
        :tooltip "Not available until all valuations have been recieved."
        :icon (get-icon :unavailable)}]]]))
