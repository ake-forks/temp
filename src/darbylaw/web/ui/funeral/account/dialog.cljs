(ns darbylaw.web.ui.funeral.account.dialog
  (:require
    [reagent-mui.components :as mui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.util :as util]
    [darbylaw.web.util.form :as form]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.funeral.account.form :as account-form]))


;; >> Events

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    (let [[query-values files] (util/split-map values [:receipt :invoice])]
      {:db (fork/set-submitting db path true)
       :http-xhrio
       (ui/build-http
         {:method :put
          :uri (str "/api/case/" case-id "/funeral/account")
          :url-params query-values
          :body (form/->FormData files)
          ;; TODO:
          :on-success [::submit-success case-id fork-params]
          :on-failure [::submit-failure case-id fork-params]})})))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id {:keys [path values]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::funeral-model/hide-funeral-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ case-id {:keys [path values]}]]
    {:db (fork/set-submitting db path false)}))



;; >> Dialog

(defn dialog
  ([{:keys [type values]}]
   (let [case-id (:case-id @(rf/subscribe [::ui/path-params]))]
     [mui/stack {:spacing 1 :sx {:padding 2}}
      [mui/stack {:direction :row :justify-content :space-between}
       [mui/typography {:variant :h5}
        (if (= type :add)
          "add funeral account"
          "edit funeral account")]
       [mui/icon-button {:on-click #(rf/dispatch [::funeral-model/hide-funeral-dialog])}
        [ui/icon-close]]]
      [account-form/form values
       #(rf/dispatch [::submit! case-id %])]])))
