(ns darbylaw.web.ui.buildingsociety.stage-notify
  (:require
    [darbylaw.web.ui :as ui]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [reagent.core :as r]))

(rf/reg-event-fx [::send-notification-success]
  (fn [{:keys [db]} [_ case-id {:keys [path]}]]
    {:fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::send-notification-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response))}))
(rf/reg-event-fx ::send-notification
  (fn [{:keys [db]} [_ case-id buildsoc-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/approve-notification")
        :params {:buildsoc-id buildsoc-id}
        :on-success [::send-notification-success case-id]
        :on-failure [::send-notification-failure]})}))

(defn submit-buttons [case-id buildsoc-id]
  ;TODO amalgamate with buttons in form ns
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between
              :sx {:width 1}}
   [mui/button {:onClick #(rf/dispatch [::model/hide-dialog])
                :variant :contained
                :full-width true}
    "cancel"]
   [mui/button {:type :submit
                :variant :contained
                :full-width true
                :startIcon (r/as-element [ui/icon-send])
                :on-click #(rf/dispatch [::send-notification case-id buildsoc-id])}
    "send letter"]])

(defn bank-notify [author]
  [mui/box
   [mui/stepper {:orientation :vertical
                 :nonLinear true
                 :activeStep 100}
    [mui/step {:key :view
               :expanded true}
     [mui/step-label
      "Review letter"]
     [mui/step-content
      [mui/typography {:variant :body1
                       :font-weight :bold}
       (cond
         (= author :unknown-user)
         "This letter was modified by a user."

         (string? author)
         (str "This letter was uploaded by " author)

         :else
         "This letter was automatically generated from case data.")]
      [mui/button {:onClick (rf/dispatch [::regenerate])
                   :startIcon (r/as-element [ui/icon-refresh])
                   :variant :outlined
                   :sx {:mt 1}}
       "Regenerate letter from case data"]
      #_[mui/stack {:direction :row
                    :alignItems :center
                    :spacing 1
                    :sx {:mt 2}}
         [mui/button {:variant :outlined
                      :startIcon (r/as-element
                                   (let [flip {:transform "scaleX(-1)"}]
                                     [ui/icon-launch {:sx flip}]))}
          "View letter here"]
         [mui/typography {:variant :body1}
          "or"]
         [mui/button {:variant :outlined
                      :startIcon (r/as-element [ui/icon-download])}
          "Download for review"]]]]
    [mui/step {:key :edit
               :expanded true}
     [mui/step-label
      "Modify letter if needed"]
     [mui/step-content
      [mui/typography {:variant :body1}
       "You can modify the letter using Word."]
      [mui/typography {:variant :body2}
       "(Be careful in keeping the first page layout intact, "
       "as the address must match the envelope's window)."]
      [mui/stack {:direction :row
                  :spacing 1
                  :sx {:mt 1}}
       [mui/button {
                    :variant :outlined
                    :startIcon (r/as-element [ui/icon-download])}
        "download current letter"]
       [mui/button {:variant :contained} "upload replacement"]]]]
    [mui/step {:key :approve
               :expanded true}
     [mui/step-label
      "Approve and send"]
     [mui/step-content
      [mui/form-control-label
       {:control (r/as-element
                   [mui/checkbox])
        :label "I approve, the letter is ready to be sent."}]
      [mui/alert {:severity :info}
       [mui/stack
        "The letter will be posted automatically through ordinary mail. "
        "We will wait for an answer from the bank for the final valuation step."]]]]]])


(defn panel []
  (let [dialog-data @(rf/subscribe [::model/get-dialog])
        buildsoc-id (:id dialog-data)
        case-id @(rf/subscribe [::case-model/case-id])]
    [mui/box shared/tall-dialog-props
     [mui/stack {:spacing 1
                 :direction :row
                 :sx {:height 1}}
      ;left side
      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [:iframe {:style {:height "100%"}
                 :src "/Example-bank-confirmation-letter.pdf"}]]

      ;right side
      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [mui/dialog-title
        [shared/header (:id dialog-data) 1]]
       [mui/dialog-content
        [bank-notify :unknown-user]]
       [mui/dialog-actions
        [submit-buttons case-id buildsoc-id]]]]]))