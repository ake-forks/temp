(ns darbylaw.web.ui.banking.stage-add
  (:require
    [darbylaw.web.ui :as ui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.form :as form]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.banking.shared :as shared]))

(rf/reg-event-fx ::add-success
  (fn [{:keys [db]} [_ case-id {:keys [path]} response]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::add-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))
(defn remove-joint [data]
  (mapv (fn [acc]
          (if (false? (:joint-check acc))
            (apply dissoc acc [:joint-check :joint-info])
            acc))
    (:accounts data)))
(defn bank-transform-on-submit [data]
  (merge {:bank-id (keyword (:bank-id data))
          :accounts (remove-joint data)}))

(rf/reg-event-fx ::add-bank
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/bank/" case-id "/add-bank-accounts")
        :params (bank-transform-on-submit values)
        :on-success [::add-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))

(defn buildsoc-transform-on-submit [values]
  (assoc values :buildsoc-id (keyword (:buildsoc-id values))))
(rf/reg-event-fx ::add-buildsoc
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/buildingsociety/" case-id "/add-buildsoc-accounts")
        :params (buildsoc-transform-on-submit values)
        :on-success [::add-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))
(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ type case-id fork-params]]
    (if (= type "bank")
      {:dispatch [::add-bank case-id fork-params]}
      {:dispatch [::add-buildsoc case-id fork-params]})))

(defn subheading [type values relationship]
  (if (= type "bank")
    [mui/typography {:variant :h5}
     (str "To the best of your knowledge, enter the details for your late "
       relationship
       (if (some? (:bank-id values))
         (str "'s accounts with " (model/asset-label "bank" (keyword (:bank-id values))))
         (str "'s accounts.")))]
    [mui/typography {:variant :h5}
     (str "To the best of your knowledge, enter the details for your late "
       relationship
       (if (some? (:buildsoc-id values))
         (str "'s accounts with " (model/asset-label "buildsoc" (keyword (:buildsoc-id values))))
         (str "'s accounts.")))]))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        type @(rf/subscribe [::model/get-type])]
    [:form {:on-submit handle-submit}
     [mui/dialog-title
      [shared/title-only (str "add a " (case type
                                         "bank" "bank"
                                         "buildsoc" "building society"))]]
     [mui/dialog-content

      [mui/box shared/narrow-dialog-props
       [mui/stack {:sx {:height 1}}
        ;padding 5 = 40px (same as icon stack ^)
        [mui/stack {:justify-content :space-between
                    :sx {:height 1}}
         [mui/stack {:spacing 2}
          (if (= type "bank")
            [form/bank-select fork-args]
            [form/buildsoc-select fork-args])
          [subheading type values (-> current-case :deceased :relationship)]
          [mui/typography {:variant :body1}
           "If you don't have any account details, don't worry
         - organisations can usually retrieve the information they need
         with just a name and date of birth.
         If this is the case please check the box below."]
          [form/accounts-unknown fork-args]
          (if (:accounts-unknown values)
            [:<>]
            [form/account-array-component type fork-args])]]]]]
     [mui/dialog-actions
      [form/submit-buttons {:left-label "cancel" :right-label "save"}]]]))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        type @(rf/subscribe [::model/get-type])]
    [form/form layout {:accounts [{}]} #(rf/dispatch [::submit! type case-id %]) {}]))


