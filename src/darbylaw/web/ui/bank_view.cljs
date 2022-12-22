(ns darbylaw.web.ui.bank-view
  (:require [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.app-layout :as c]
            [darbylaw.web.styles :as styles]
            [darbylaw.api.bank-list :as bank-list]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-model :as bank-model]))

(rf/reg-event-fx
  ::load-success
  (fn [{:keys [db]} [_ response]]
    {:db (assoc db :current-case response)}))

(rf/reg-event-db
  ::load-failure
  (fn [db [_ case-id result]]
    (assoc db :failure-http-result result :case-id case-id)))

(rf/reg-event-fx ::get-case!
  (fn [_ [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id)
        :on-success [::load-success]
        :on-failure [::load-failure case-id]})}))

(rf/reg-event-fx ::load!
  (fn [_ [_ case-id]]
    {:dispatch [::get-case! case-id]}))

(rf/reg-sub ::route-params
  (fn [db _]
    (:path-params (:kee-frame/route db))))

(rf/reg-sub ::user-banks
  (fn [db _]
    (:bank-accounts (:current-case db))))

(defn display-accounts [accounts]
  [mui/stack {:spacing 1 :style {:margin-top "1rem"}}
   (map (fn [acc]
          [mui/stack {:spacing 1}
           [mui/stack {:spacing 1 :direction :row :justify-content :space-between}
            [mui/text-field {:label "sort code" :value (:sort-code acc) :disabled true}]
            [mui/text-field {:label "account number" :value (:account-number acc) :disabled true}]
            [mui/text-field {:label "estimated value" :value (:estimated-value acc) :disabled true}]
            [mui/button
             {:start-icon (r/as-element [ui/icon-edit])}]
            [mui/button
             {:start-icon (r/as-element [ui/icon-delete])}]]
           [mui/stack {:spacing 1 :direction :row :style {:width "80%"}}
            (if (some? (:joint-check acc))
              [mui/form-control-label {:label "joint account"
                                       :control (r/as-element [mui/checkbox {:defaultChecked true
                                                                             :disabled true}])}])

            (if (some? (:joint-info acc))
              [mui/text-field {:label "joint account info"
                               :value (:joint-info acc)
                               :disabled true
                               :full-width true}])]])
     accounts)])

(rf/reg-sub ::notification-process-starting?
  (fn [db [_ case-id bank-id]]
    (boolean (get-in db [:current-case :bank bank-id ::notification-process-starting?]))))

(defn start-notification-process-button [case-id bank-id]
  [ui/loading-button {:onClick #(rf/dispatch [::bank-model/generate-notification-letter case-id bank-id])
                      :sx (when @(rf/subscribe [::bank-model/start-notification-hidden? case-id bank-id])
                            {:display :none})
                      :loading @(rf/subscribe [::notification-process-starting? case-id bank-id])
                      :variant :contained}
   "start bank notification process"])

(rf/reg-event-fx ::cancel-notification-process--success
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:dispatch [::load! case-id]}))

(rf/reg-event-fx ::cancel-notification-process
  (fn [{:keys [db]} [_ case-id bank-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/cancel-notification")
        :on-success [::cancel-notification-process--success case-id bank-id]})}))

(rf/reg-sub ::cancel-notification-hidden?
  (fn [db [_ case-id bank-id]]
    (not (bank-model/ongoing-notification-process? db bank-id))))

(defn cancel-notification-process-button [case-id bank-id]
  [mui/button {:onClick #(rf/dispatch [::cancel-notification-process case-id bank-id])
               :sx (when @(rf/subscribe [::cancel-notification-hidden? case-id bank-id])
                     {:display :none})
               :variant :text
               :startIcon (r/as-element [ui/icon-warning])}
   "cancel bank notification process"])

(rf/reg-sub ::review-notification-pdf-disabled?
  (fn [db [_ case-id bank-id]]
    (not (bank-model/ongoing-notification-process? db bank-id))))

(defn review-notification-pdf-button [case-id bank-id]
  [mui/button {:href (str "/api/case/" case-id "/bank/" (name bank-id) "/notification-pdf")
               :disabled @(rf/subscribe [::review-notification-pdf-disabled? case-id bank-id])
               :target "_blank"
               :variant :contained
               :endIcon (r/as-element [ui/icon-open-in-new])}
   "review notification letter pdf"])

(defn display-timeline []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        bank-id (-> @(rf/subscribe [::route-params]) :bank-id keyword)]
    [mui/stepper {:orientation "vertical"
                  :active-step 2}
     [mui/step {:expanded true}
      [mui/step-label "you have completed all account information"]
      [mui/step-content
       [mui/typography
        "You won't be able to change account information for this bank as soon as you start the bank notification process."]]
      [mui/step-content
       [start-notification-process-button case-id bank-id]
       [cancel-notification-process-button case-id bank-id]]]
     [mui/step {:expanded true}
      [mui/step-label "we have generated the notification letter"]
      [mui/step-content
       [review-notification-pdf-button case-id bank-id]]]
     [mui/step {:expanded true}
      [mui/step-label "we are waiting to receive confirmation and valuations from the bank"]
      [mui/step-content
       [mui/button {:variant :contained :disabled true} "view letter received PDF"]]]
     [mui/step {:expanded true}
      [mui/step-label "we will confirm the finalised valuations for these accounts"]
      [mui/step-content
       [mui/button {:variant :contained :disabled true} "enter valuations"]]]]))


(comment
  (def users-banks [{:id :barclays-bank-plc, :accounts [{:sort-code "1", :account-number "1", :estimated-value "150"}
                                                        {:sort-code "2", :account-number "2", :estimated-value "300"}]}
                    {:id :aberdeen-standard-investments, :accounts [{:sort-code "1", :account-number "1", :estimated-value "100"}
                                                                    {:sort-code "2", :account-number "2", :estimated-value "200"}
                                                                    {:sort-code "2", :account-number "2", :estimated-value "300"}
                                                                    {:sort-code "1", :account-number "1", :estimated-value "5000"}]}
                    {:id :citibank-international-plc, :accounts [{:sort-code "1", :account-number "1", :estimated-value "1000"}]}]))

(defn display-info []
  (let [case-id (-> @(rf/subscribe [::route-params])
                  :case-id)
        bank-id (-> @(rf/subscribe [::route-params])
                  :bank-id
                  keyword)
        current-bank-static-data (bank-list/bank-by-id bank-id)
        all-user-banks @(rf/subscribe [::user-banks])
        current-user-bank (filter #(= (:id %) bank-id) all-user-banks)]
    (assert case-id)
    (rf/dispatch [::load! case-id])
    [mui/container {:class (styles/main-content)}
     [mui/stack {:spacing 1}
      [mui/stack {:direction :row :justify-content :space-between}
       [mui/typography {:variant :h3} (:common-name current-bank-static-data)]
       [mui/stack {:spacing 0.5}
        [mui/typography {:variant :h5 :sx {:color "#B08BBF"}} (str "total estimated value £"
                                                                (reduce + (map #(js/parseFloat (:estimated-value %)) (:accounts current-user-bank))))]
        [mui/typography {:variant :h5 :sx {:color "#E0711C"}} "total confirmed value £0"]]]
      [mui/divider]
      [mui/stack {:spacing 5 :direction :row}
       [mui/stack {:spacing 1 :style {:max-width "60%"}}
        [mui/typography {:variant :h5} "progress summary"]
        [mui/typography {:variant :p}
         (str "On the right is an overview for the current progress you've made in finalising
         the accounts with " (:common-name current-bank-static-data) ".
         You can view and download all the correspondence and documentation sent and
         received at each stage of the process by clicking through the steps.")]
        [mui/typography {:variant :p}
         "Below are all the accounts that you have informed us of, and you can edit these details up
         until we receive the bank's valuations."]
        [mui/typography {:variant :p :style {}}
         "Something doesn't look right? "
         [mui/link "Get in touch"] " for assistance."]]
       [display-timeline]]
      [display-accounts (:accounts (first current-user-bank))]]]))

(defn panel []
  [mui/container
   [c/navbar]
   [display-info]
   [c/footer]])

(defmethod routes/panels :view-bank-panel [] [panel])

