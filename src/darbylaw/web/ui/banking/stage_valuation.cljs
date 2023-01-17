(ns darbylaw.web.ui.banking.stage-valuation
  (:require
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.banking.form :as form]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.shared :as shared]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-fx ::value-success
  (fn [{:keys [db]} [_ case-id {:keys [path]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::value-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::value-buildsoc-accounts
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/value-buildsoc-accounts")
        :params values
        :on-success [::value-success case-id fork-params]
        :on-failure [::value-failure fork-params]})}))

(rf/reg-event-fx ::value-bank-accounts
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/bank/" case-id "/update-bank-accounts")
        :params (model/bank-transform-on-submit values)
        :on-success [::value-success case-id fork-params]
        :on-failure [::value-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ type case-id fork-params]]
    (case type
      :bank {:dispatch [::value-bank-accounts case-id fork-params]}
      :buildsoc {:dispatch [::value-buildsoc-accounts case-id fork-params]})))

(defn accounts-valued? [accounts]
  (and
    (not-empty accounts)
    (every? (fn [acc] (some? (:confirmed-value acc))) accounts)))

(defn layout [{:keys [handle-submit values] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        case-id @(rf/subscribe [::case-model/case-id])
        type @(rf/subscribe [::model/current-banking-type])
        asset-id @(rf/subscribe [::model/current-banking-id])
        asset-label (model/asset-label type asset-id)
        valuation-letter @(rf/subscribe [::model/current-valuation-letter])]
    [:form {:on-submit handle-submit}
     [mui/box shared/tall-dialog-props
      [mui/stack {:spacing 1
                  :direction :row
                  :sx {:height 1}}
       ;left side
       [mui/stack {:spacing 1 :sx {:width 0.5}}
        (if valuation-letter
          [:iframe {:style {:height "100%"}
                    :src (str "/api/case/" case-id "/" (name type) "/" (name asset-id) "/valuation-pdf")}]
          [mui/typography {:variant :h6} "upload a PDF of the valuation"])]

       ;right side
       [mui/stack {:spacing 1 :sx {:width 0.5}}
        [mui/dialog-title
         [shared/header type asset-id :valuation]]
        [mui/dialog-content
         [mui/stack {:spacing 2}
          [mui/typography {:variant :body1}
           (str "Once you have received a letter from "
             asset-label
             ", upload a scanned pdf using the button below.")]
          [shared/upload-button type case-id asset-id
           {:variant :outlined
            :full-width true
            :startIcon (r/as-element [ui/icon-upload])}
           "upload pdf"
           "/valuation-pdf"]
          [mui/typography {:variant :body1}
           (str "Please enter the confirmed details for each of your late "
             (-> current-case :deceased :relationship)
             "'s accounts with "
             asset-label ".")]
          [form/account-array-component type (merge fork-args {:stage :valuation})]]]
        [mui/dialog-actions
         [form/submit-buttons {:left-label "cancel" :right-label "submit valuations"
                               :right-disabled (not (and (accounts-valued? (:accounts values))
                                                         valuation-letter))}]]]]]]))


(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        type @(rf/subscribe [::model/current-banking-type])
        values @(rf/subscribe [::model/current-asset-data])]
    [form/form layout values #(rf/dispatch [::submit! type case-id %])]))
