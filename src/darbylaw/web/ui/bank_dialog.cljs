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
            [darbylaw.web.ui.bank-letter-approval :as bank-letter-approval]))



(rf/reg-sub ::bank-data
  :<- [::case-model/current-case]
  :<- [::bank-model/bank-id]
  (fn [[current-case bank-id]]
    (get-in current-case [:bank bank-id])))

(rf/reg-sub ::stage
  :<- [::bank-data]
  (fn [bank-data]
    (cond
      (= (:notification-status bank-data) :started)
      :approve-letter
      (= (:notification-status bank-data) :notification-letter-sent)
      :confirm-values
      (and (= (:notification-status bank-data) :values-confirmed))
      :bank-completed
      :else
      :edit-accounts)))

;grey :unavailable
;black :available
;orange :tasks-available
;loading :waiting-on-us
;green ;completed

;case stages
;:edit-accounts
;:approve-letter
;:confirm-values
;:completed

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
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        stage @(rf/subscribe [::stage])]
    [mui/card {:style {:padding "1rem"}}
     [mui/stepper {:alternative-label true}
      (for [{:keys [label status-fn]} bank-steps]
        (let [status (status-fn stage)]
          [mui/step {:completed (= status :completed)}
           [mui/step-label {:icon (progress-bar/get-icon status)}
            [mui/typography {:variant :body2
                             :style {:textTransform :uppercase}}
             label]]]))]]))

(defn dialog-header [bank-id]
  [mui/stack {:spacing 1.5}
   [mui/typography {:variant :h4} (list/bank-label bank-id)
    [bank-progress-bar]]])

(defn edit-dialog []
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id @(rf/subscribe [::case-model/case-id])
        current-bank (filter #(= (:id %) bank-id)
                       (:bank-accounts @(rf/subscribe [::case-model/current-case])))]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "700px"
                        :width "1130px"}}
     [dialog-header bank-id]
     [bank-add/dialog-with-values
      (if (some? bank-id)
        {:accounts (:accounts (first current-bank))
         :bank-id (name bank-id)})]
     [mui/button {:on-click #(rf/dispatch [::bank-model/start-notification-process case-id bank-id])
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
      [mui/box {:style {:width "50%"}}
       [:iframe {:src "/Example-bank-confirmation-letter.pdf" :width "100%" :height "100%"}]]
      [mui/stack {:spacing 1 :style {:width "50%"}}
       [mui/typography {:variant :h4} (list/bank-label bank-id)]
       [bank-progress-bar]
       [confirmation-view/bank-confirmation-panel]]]]))

(defn bank-completed-dialog []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        current-bank (filter #(= (:id %) bank-id) banks)]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "700px"
                        :width "1130px"}}
     [dialog-header bank-id]
     [mui/stack {:direction :row :spacing 1}
      [mui/stack {:spacing 2 :width "50%"}
       [mui/typography {:variant :h6}
        "The notification process is complete.
        You can view copies of all correspondence using the buttons below."]
       [mui/box
        [mui/typography "todo -> add buttons to display pdfs"]]]
      [mui/stack {:direction :row :spacing 1 :style {:align-self :baseline}}
       [mui/box
        [mui/typography "accounts"]
        (map
          (fn [acc]
            [mui/stack {:direction :row :spacing 1}
             [mui/typography {:variant :h6} (str "sort code: " (:sort-code acc))]
             [mui/typography {:variant :h6} (str "account number: " (:account-number acc))]
             [mui/typography {:variant :h6} (str "value: £" (:confirmed-value acc))]])
          (:accounts (first current-bank)))]]]
     [mui/button {:on-click #(rf/dispatch [::bank-model/hide-bank-dialog])
                  :variant :contained} "close"]]))

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
