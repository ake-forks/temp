(ns darbylaw.web.ui.bank-dialog
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent-mui.components :as mui]
            [darbylaw.api.bank-list :as list]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-model :as bank-model]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.progress-bar :as progress-bar]
            [darbylaw.web.ui.bank-add :as bank-add]
            [darbylaw.web.ui.bank-confirmation-view :as confirmation-view]
            [darbylaw.web.ui.bank-letter-approval :as bank-letter-approval]
            [darbylaw.web.ui.document-view :as document-view]))

(rf/reg-sub ::stage
  :<- [::bank-model/current-bank-data]
  (fn [bank-data]
    (cond
      (:values-confirmed bank-data)
      :bank-completed

      (get-in bank-data [:notification-letter :approved])
      :confirm-values

      (:notification-letter bank-data)
      :approve-letter

      :else
      :edit-accounts)))

;grey :unavailable
;black :available
;orange :tasks-available
;loading :waiting-on-us
;green ;completed

(def bank-steps
  "An array of maps representing a step

  Each step can have the following:
  :label      The label of the step (required)
  :status-fn  A function that, given the current-case, returns a status for that step
              (see the get-icon function) (required)"
  [{:label "add accounts"
    :status-fn (fn [stage]
                 (case stage
                   :edit-accounts :tasks-available
                   :approve-letter :completed
                   :confirm-values :completed
                   :bank-completed :completed
                   :tasks-available))}
   {:label "send notification"
    :status-fn (fn [stage]
                 (case stage
                   :edit-accounts :unavailable
                   :approve-letter :tasks-available
                   :confirm-values :completed
                   :bank-completed :completed
                   :unavailable))}
   {:label "input valuations"
    :status-fn (fn [stage]
                 (case stage
                   :edit-accounts :unavailable
                   :approve-letter :unavailable
                   :confirm-values :tasks-available
                   :bank-completed :completed
                   :unavailable))}])

(defn bank-progress-bar []
  (let [stage @(rf/subscribe [::stage])]
    [mui/card {:style {:padding "1rem"}}
     [mui/stepper {:alternative-label true}
      (into [:<>]
        (for [{:keys [label status-fn]} bank-steps]
          (let [status (status-fn stage)]
            ^{:key label}
            [mui/step {:completed (= status :completed)}
             [mui/step-label {:icon (progress-bar/get-icon status)}
              [mui/typography {:variant :body2
                               :style {:textTransform :uppercase}}
               label]]])))]]))

(defn dialog-header [bank-id]
  [mui/stack {:spacing 1.5}
   [mui/typography {:variant :h4} (list/bank-label bank-id)
    [bank-progress-bar]]])

(defn edit-dialog []
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id @(rf/subscribe [::case-model/case-id])
        current-bank @(rf/subscribe [::bank-model/current-bank-data])]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "700px"
                        :width "1130px"}}
     [dialog-header bank-id]
     [bank-add/dialog-with-values
      (if (some? bank-id)
        {:accounts (:accounts current-bank)
         :bank-id (name bank-id)})]
     [mui/button {:on-click #(rf/dispatch [::bank-model/generate-notification-letter case-id bank-id])
                  :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                  :variant :text
                  :size "large"
                  :full-width false
                  :start-icon (r/as-element [ui/icon-check])}
      "mark accounts complete"]]))

(defn approve-letter-pdf [uploading? case-id bank-id]
  [mui/box {:style {:width "50%"}}
   (if (false? @uploading?)
     [:iframe {:src (str "/api/case/" case-id "/bank/" (name bank-id) "/notification-pdf")
               :width "100%"
               :height "100%"}]
     [reagent-mui.lab.loading-button/loading-button {:loading true :full-width true}])])

(defn approve-letter-dialog []
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id @(rf/subscribe [::case-model/case-id])]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "90vh"
                        :width "90vw"}}
     [mui/stack {:direction :row :spacing 1 :style {:height "inherit"}}
      [approve-letter-pdf bank-letter-approval/uploading? case-id bank-id]
      [mui/stack {:spacing 1 :style {:width "50%"}}
       [mui/typography {:variant :h4} (list/bank-label bank-id)]
       [bank-progress-bar]
       [bank-letter-approval/panel]]]]))

(defn confirm-values-dialog []
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "90vh"
                        :width "90vw"}}
     [mui/stack {:direction :row :spacing 2 :style {:height "inherit"}}
      [mui/box {:width "50%"}
       [confirmation-view/upload-valuation-pdf]]
      [mui/stack {:spacing 1 :style {:width "50%"}}
       [mui/typography {:variant :h4} (list/bank-label bank-id)]
       [bank-progress-bar]
       [confirmation-view/bank-confirmation-panel]]]]))

(defn bank-completed-dialog []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        case-id (:id current-case)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        current-bank @(rf/subscribe [::bank-model/current-bank-data])]
    [mui/stack {:spacing 1
                :direction :row
                :style {:padding "2rem"
                        :background-color :white}}


     [mui/box {:style {:width "50%"}}
      [document-view/view-pdf-dialog
       {:buttons [{:key "value"
                   :name "value confirmation"
                   :source (str "/api/case/" case-id "/bank/" (name bank-id) "/valuation-pdf")}
                  {:key "notification"
                   :name "notification letter"
                   :source (str "/api/case/" case-id "/bank/" (name bank-id) "/notification-pdf")}]}]]

     [mui/stack {:spacing 2 :style {:width "50%"}}
      [dialog-header bank-id]

      [mui/stack {:spacing 1
                  :flex-grow 1
                  :justify-content :space-between}
       [mui/box
        [mui/typography {:variant :body1}
         "The notification process is complete.
       You can view copies of all correspondence using the buttons to the left."]
        [mui/typography {:variant :h6} "accounts summary"]
        (map
          (fn [acc]
            [mui/stack {:direction :row :spacing 4}
             [mui/typography {:variant :body2} (:sort-code acc)]
             [mui/typography {:variant :body2} (:account-number acc)]
             [mui/typography {:variant :body2} (:confirmed-value acc)]])
          (:accounts (first current-bank)))]
       [mui/button
        {:variant :contained
         :full-width true
         :on-click #(do (rf/dispatch [::document-view/hide-pdf])
                        (rf/dispatch [::bank-model/hide-bank-dialog]))}
        "close"]]]]))

(defn base-dialog []
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)]
    (rf/dispatch [::case-model/load-case! case-id])
    [mui/box
     (if (= bank-id :add-bank)
       [bank-add/dialog]
       (let [stage @(rf/subscribe [::stage])]
         (cond
           (= stage :edit-accounts) [edit-dialog]
           (= stage :approve-letter) [approve-letter-dialog]
           (= stage :confirm-values) [confirm-values-dialog]
           (= stage :bank-completed) [bank-completed-dialog])))]))
