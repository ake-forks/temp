(ns darbylaw.web.ui.pensions.add
  (:require
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.shared :as shared]
    [darbylaw.web.ui.pensions.form :as form]
    [darbylaw.web.ui :as ui :refer (<<)]))

(rf/reg-event-fx ::add-success
  (fn [{:keys [db]} [_  case-id {:keys [path]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::add-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::add-pension
  (fn [{:keys [db]} [_ pension-type case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri
        (case pension-type
          :private (str "/api/case/" case-id "/pension/add-private")
          "")
        :params (assoc values :provider (keyword (:provider values)))
        :on-success [::add-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))

(defn layout [{:keys [handle-submit] :as fork-args}]
  (let [dialog (<< ::model/dialog)]
    [:form {:on-submit handle-submit}
     [mui/dialog-content {:style shared/dialog-size}
      [shared/dialog-header "add a private pension"]
      [mui/stack {:spacing 2}
       [form/company-select fork-args]
       [form/ni-field fork-args]
       [form/ref-field fork-args]
       [mui/typography {:variant :body1 :style {:margin-top "0.5rem"}}
        "Please provide a reference or policy number if known."]]]
     [mui/dialog-actions
      [mui/button {:type :submit} "save"]]]))

(defn panel []
  (let [case-id (<< ::case-model/case-id)
        dialog (<< ::model/dialog)]
    [form/form {:layout layout
                :submit-fn #(rf/dispatch [::add-pension (:pension-type dialog) case-id %])}]))