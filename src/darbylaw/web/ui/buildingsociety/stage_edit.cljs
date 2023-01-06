(ns darbylaw.web.ui.buildingsociety.stage-edit
  (:require
    [darbylaw.web.ui :as ui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-fx ::complete-buildsoc-success
  (fn [{:keys [db]} [_ case-id {:keys [path values]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/generate-notification case-id (:buildsoc-id values)]]
          [:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::complete-buildsoc-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(defn transform-on-submit [values]
  (if (= true (:accounts-unknown values))
    {:buildsoc-id (keyword (:buildsoc-id values)) :accounts [] :accounts-unknown true}
    {:buildsoc-id (keyword (:buildsoc-id values)) :accounts (:accounts values) :accounts-unknown false}))

(rf/reg-event-fx ::complete-buildsoc
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/complete-buildsoc-accounts")
        :params (transform-on-submit values)
        :on-success [::complete-buildsoc-success case-id fork-params]
        :on-failure [::complete-buildsoc-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:dispatch [::complete-buildsoc case-id fork-params]}))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        buildsoc-id @(rf/subscribe [::model/current-buildsoc-id])]
    [:form {:on-submit handle-submit}
     [mui/dialog-title
      [shared/header buildsoc-id 0]]
     [mui/dialog-content
      [mui/box shared/narrow-dialog-props
       [mui/stack {:justify-content :space-between
                   :sx {:height 1}}
        [mui/stack {:spacing 1}
         [mui/typography {:variant :h5} "add accounts"]
         [mui/typography {:variant :body1}
          (str "To the best of your knowledge, enter the details for your late "
            (-> current-case :deceased :relationship)
            (if-let [name (model/buildsoc-label buildsoc-id)]
              (str "'s accounts with " name)
              "'s accounts."))]
         [form/accounts-unknown fork-args]
         (if (:accounts-unknown values)
           [:<>]
           [form/account-array-component fork-args])]]]]
     [mui/dialog-actions
      [shared/submit-buttons {:left-label "cancel" :right-label "accounts complete"}]]]))


(defn panel []
  (let [values @(rf/subscribe [::model/current-buildsoc-data])
        case-id @(rf/subscribe [::case-model/case-id])]
    [form/form layout values #(rf/dispatch [::submit! case-id %])]))