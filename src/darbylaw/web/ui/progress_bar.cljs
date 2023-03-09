(ns darbylaw.web.ui.progress-bar
  (:require
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [re-frame.core :as rf]
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
  (let [current-case @(rf/subscribe [::current-case])
        institutions (:bank-accounts current-case)]
    [mui/card
     [mui/stepper {:alternative-label true
                   :non-linear true
                   :style {:margin-top "2rem"
                           :margin-bottom "2rem"}}
      (stepper-item
       {:label "Case Created"
        :icon (get-icon :completed)})
      [stepper-item 
       {:label "Identity Check"
        :tooltip (case @(rf/subscribe [::identity-model/current-final-result])
                   :unknown "We're waiting on an admin to run the checks."
                   :pass "Identity checks have passed."
                   "Some manual intervention is required.")
        :icon [identity-dialog/check-icon]}]
      (let [status (cond
                     (and (some? institutions)
                          (every? :notification-letter institutions))
                     :completed

                     (some? institutions)
                     :tasks-available

                     :else :available)]
        [stepper-item 
         {:label "Complete Assets"
          :tooltip (case status
                     :available "Add bank and utility assets via the dashboard."
                     :tasks-available "One or more banks left to complete."
                     :completed "All added banks have been complete!")
          :icon (get-icon status)}])
      (let [status (cond
                     (and (some? institutions)
                          (every? (comp :sent-at :notification-letter) institutions))
                     :completed

                     (some :notification-letter institutions)
                     :tasks-available

                     (some? institutions)
                     :available

                     :else :unavailable)]
        [stepper-item 
         {:label "Notify Institutions"
          :tooltip (case status
                     :unavailable "We will trigger a notification when one or more assets are complete."
                     :available "Waiting on an admin to generate notifications for completed assets."
                     :tasks-available "Waiting on an admin to send notifications for completed assets."
                     :completed "All completed assets have had notification letters sent!")
          :icon (get-icon status)}])
      (let [status (cond
                     (and (some? institutions)
                          (every? :valuation-letter institutions)
                          (every? (comp (complement empty?) :accounts) institutions)
                          (->> institutions
                              (mapcat :accounts)
                              (every? :confirmed-value)))
                     :completed

                     (some :valuation-letter institutions)
                     :tasks-available

                     (some :notification-letter institutions)
                     :available

                     :else :unavailable)]
        [stepper-item 
         {:label "Receive Valuations"
          :tooltip (case status
                     :unavailable "Not available until one or more institutions have been notified."
                     :available "Waiting on receipt of a valuation letter from one or more institutions."
                     :tasks-available "Complete the valuation of the available assets."
                     :completed "All assets have been valued!")
          :icon (get-icon status)}])
      [stepper-item 
       {:label "Grant of Probate"
        :tooltip "Not available until all valuations have been received."
        :icon (get-icon :unavailable)}]]]))
