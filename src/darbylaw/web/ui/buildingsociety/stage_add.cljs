(ns darbylaw.web.ui.buildingsociety.stage-add
  (:require
    [darbylaw.web.ui :as ui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.buildingsociety.shared :as shared]))

(rf/reg-event-fx ::add-buildsoc-success
  (fn [{:keys [db]} [_ case-id {:keys [path]} response]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::add-buildsoc-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(defn transform-on-submit [values]
  (assoc values :buildsoc-id (keyword (:buildsoc-id values))))
(rf/reg-event-fx ::add-buildsoc
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/add-buildsoc-accounts")
        :params (transform-on-submit values)
        :on-success [::add-buildsoc-success case-id fork-params]
        :on-failure [::add-buildsoc-failure fork-params]})}))
(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:dispatch [::add-buildsoc case-id fork-params]}))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])]
    [:form {:on-submit handle-submit}
     [mui/dialog-title
      [shared/title-only "add a building society"]]
     [mui/dialog-content
      [mui/box shared/narrow-dialog-props
       [mui/stack {:sx {:height 1}}
        ;padding 5 = 40px (same as icon stack ^)
        [mui/stack {:justify-content :space-between
                    :sx {:height 1}}
         [mui/stack {:spacing 2}

          [form/buildsoc-select fork-args]
          [mui/typography {:variant :h5}
           (str "To the best of your knowledge, enter the details for your late "
             (-> current-case :deceased :relationship)
             (if-let [name (:buildsoc-id values)]
               (str "'s accounts with " name)
               "'s accounts."))]
          [mui/typography {:variant :body1}
           "If you don't have any account details, don't worry
         - building societies can usually retrieve the information they need
         with just a name and date of birth.
         If this is the case please check the box below."]
          [form/accounts-unknown fork-args]
          (if (:accounts-unknown values)
            [:<>]
            [form/account-array-component fork-args])]]]]]
     [mui/dialog-actions
      [form/submit-buttons {:left-label "cancel" :right-label "save"}]]]))



(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])]
    [form/form layout {:accounts [{}]} #(rf/dispatch [::submit! case-id %])]))


