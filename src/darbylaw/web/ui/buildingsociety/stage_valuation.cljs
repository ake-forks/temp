(ns darbylaw.web.ui.buildingsociety.stage-valuation
  (:require
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-fx ::value-buildsoc-success
  (fn [{:keys [db]} [_ case-id {:keys [path]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::value-buildsoc-failure
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
        :on-success [::value-buildsoc-success case-id fork-params]
        :on-failure [::value-buildsoc-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:dispatch [::value-buildsoc-accounts case-id fork-params]}))

(defn layout [{:keys [handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        dialog-data @(rf/subscribe [::model/get-dialog])
        case-id @(rf/subscribe [::case-model/case-id])
        buildsoc-id @(rf/subscribe [::model/current-buildsoc-id])]
    [:form {:on-submit handle-submit}
     [mui/box shared/tall-dialog-props
      [mui/stack {:spacing 1
                  :direction :row
                  :sx {:height 1}}
       ;left side
       [mui/stack {:spacing 1 :sx {:width 0.5}}
        [:iframe {:style {:height "100%"}
                  :src (str "/api/case/" case-id "/buildsoc/" (name buildsoc-id) "/valuation-pdf")}]]

       ;right side
       [mui/stack {:spacing 1 :sx {:width 0.5}}
        [mui/dialog-title
         [shared/header (:id dialog-data) 2]]
        [mui/dialog-content
         [mui/stack {:spacing 2}
          [mui/typography {:variant :body1}
           (str "Once you have received a letter from "
             buildsoc-id
             ", upload a scanned pdf using the button below.")]
          [shared/upload-button case-id buildsoc-id
           {:variant :outlined
            :full-width true
            :startIcon (r/as-element [ui/icon-upload])}
           "upload pdf"
           "/valuation-pdf"]
          [mui/typography {:variant :body1}
           (str "Please enter the confirmed details for each of your late "
             (-> current-case :deceased :relationship)
             "'s accounts with "
             buildsoc-id ".")]
          [form/account-array-component (merge fork-args {:stage :valuation})]]]
        [mui/dialog-actions
         [form/submit-buttons {:left-label "cancel" :right-label "submit valuations"}]]]]]]))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        values @(rf/subscribe [::model/current-buildsoc-data])]
    [form/form layout values #(rf/dispatch [::submit! case-id %])]))