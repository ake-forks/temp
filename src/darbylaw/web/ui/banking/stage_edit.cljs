(ns darbylaw.web.ui.banking.stage-edit
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.banking.validation :as validation]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.shared :as shared]
    [darbylaw.web.ui.banking.form :as form]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-fx ::complete-success
  (fn [{:keys [db]} [_ case-id {:keys [path values]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/generate-notification case-id values]]
          [:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::complete-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::complete-bank
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/bank/" case-id "/update-bank-accounts")
        :params (model/bank-transform-on-submit values)
        :on-success [::complete-success case-id fork-params]
        :on-failure [::complete-failure fork-params]})}))
(rf/reg-event-fx ::complete-buildsoc
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/complete-buildsoc-accounts")
        :params (model/buildsoc-transform-on-submit values)
        :on-success [::complete-success case-id fork-params]
        :on-failure [::complete-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ type case-id fork-params]]
    (case type
      :bank {:dispatch [::complete-bank case-id fork-params]}
      :buildsoc {:dispatch [::complete-buildsoc case-id fork-params]})))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        asset-id @(rf/subscribe [::model/current-asset-id])
        type @(rf/subscribe [::model/get-type])]
    [:form {:on-submit handle-submit}
     [mui/dialog-title
      [shared/header type asset-id :edit]]
     [mui/dialog-content
      [mui/box shared/narrow-dialog-props
       [mui/stack {:justify-content :space-between
                   :sx {:height 1}}
        [mui/stack {:spacing 1}
         [mui/typography {:variant :h5} "add accounts"]
         [mui/typography {:variant :body1}
          (str "To the best of your knowledge, enter the details for your late "
            (-> current-case :deceased :relationship)
            (if-let [name (model/asset-label type asset-id)]
              (str "'s accounts with " name)
              "'s accounts."))]
         (if (= type :buildsoc)
           [form/accounts-unknown fork-args])
         (if (:accounts-unknown values)
           [:<>]
           [form/account-array-component type fork-args])]]]]
     [mui/dialog-actions
      [shared/submit-buttons {:left-label "cancel" :right-label "accounts complete"}]]]))


(defn panel []
  (let [type @(rf/subscribe [::model/get-type])
        case-id @(rf/subscribe [::case-model/case-id])
        values (model/get-asset-data type)]
    [form/form layout values #(rf/dispatch [::submit! type case-id %])
     (case type
       :bank validation/add-bank-validation
       :buildsoc validation/add-buildsoc-validation)]))
