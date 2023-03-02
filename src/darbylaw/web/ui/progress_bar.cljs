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
    (r/as-element
      [:img {:src "/images/grey-mui-leaf.png" :width "30px"}])
    ;; The user has tasks to do, or needs to kick of the step?
    ;; NOTE: Is this one needed?
    :available
    (r/as-element
      [:img {:src "/images/black-mui-leaf.png" :width "30px"}])
    ;; The user has tasks to do
    :tasks-available
    (r/as-element
      [:img {:src "/images/orange-mui-leaf.png" :width "30px"}])
    ;; Waiting on a background process to complete
    :waiting-on-us
    (r/as-element
      [:img {:src "/images/loading-step.png" :width "25px"}])
    ;; Step complete, no further action required
    :completed
    (r/as-element
      [:img {:src "/images/green-mui-leaf.png" :width "30px"}])))

(def steps
  "An array of maps representing a step
  
  Each step can have the following:
  :label      The label of the step (required)
  :status-fn  A function that, given the current-case, returns a status
              (see the get-icon function) (required)
  :tooltip    An optional tooltip to describe the step"
  [{:label "Case Created"
    :status-fn (constantly :completed)} 
   {:label "Identity Check"
    :tooltip (fn [& _]
               (case @(rf/subscribe [::identity-model/current-final-result])
                 :unknown "We're waiting on an admin to run the checks."
                 :pass "Identity checks have passed."
                 "Some manual intervention is required."))
    :status-fn (fn [& _]
                 (r/as-element
                   [identity-dialog/check-icon]))}
   {:label "Complete Assets"
    :tooltip "Add bank and utitity assets via the dashboard."
    :status-fn (fn [current-case]
                 (if-not (some? (:bank-accounts current-case))
                   :available
                   :tasks-available))}
   {:label "Notify Institutions"
    :tooltip "We will trigger a notification when one or more assets are complete."
    :status-fn (fn [current-case]
                 (if (some? (:bank-accounts current-case))
                   :available
                   :unavailable))}
   {:label "Receive Valuations"
    :tooltip "Not available until one or more institutions have been notified."
    :status-fn (constantly :unavailable)}
   {:label "Grant of Probate"
    :tooltip "Not available until all valuations have been recieved."
    :status-fn (constantly :unavailable)}])

(defn progress-bar []
  (let [current-case @(rf/subscribe [::current-case])]
    [mui/card
     [mui/stepper {:alternative-label true :non-linear true
                   :style {:margin-top "2rem" :margin-bottom "2rem"}}
      (into [:<>]
        (for [{:keys [label tooltip status-fn]} steps]
          (let [status (status-fn current-case)
                elem
                [mui/step {:completed (= status :completed)}
                 [mui/step-label {:icon (if (keyword? status)
                                          (get-icon status)
                                          status)}
                  [mui/typography {:variant :body2
                                   :style {:textTransform :uppercase}}
                   label]]]]
            (with-meta
              (if tooltip
                [mui/tooltip {:title (if (fn? tooltip)
                                       (tooltip current-case)
                                       tooltip)
                              :position :top}
                 elem]
                elem)
              {:key label}))))]]))
