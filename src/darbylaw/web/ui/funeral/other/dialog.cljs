(ns darbylaw.web.ui.funeral.other.dialog
  (:require
    [reagent-mui.components :as mui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.util :as util]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.funeral.other.form :as other-form]))


;; >> Add Expense

(rf/reg-event-fx ::add-expense
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    (let [[query-values files] (util/split-map values [:receipt])]
      {:db (fork/set-submitting db path true)
       :http-xhrio
       (ui/build-http
         {:method :post
          :uri (str "/api/case/" case-id "/funeral/other")
          :url-params query-values
          :body (util/->FormData files)
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



;; >> Edit Expense

(rf/reg-event-fx ::edit-expense
  (fn [{:keys [db]} [_ case-id expense-id
                     {:keys [path values] :as fork-params}]]
    (let [[query-values files] (util/split-map values [:receipt])]
      {:db (fork/set-submitting db path true)
       :http-xhrio
       (ui/build-http
         {:method :put
          :uri (str "/api/case/" case-id "/funeral/other/" expense-id)
          :url-params query-values
          :body (util/->FormData files) 
          ;; TODO:
          :on-success [::submit-success case-id fork-params]
          :on-failure [::submit-failure case-id fork-params]})})))



;; >> Dialog

(defn dialog
  ([{:keys [type values]}]
   (let [case-id (:case-id @(rf/subscribe [::ui/path-params]))]
     [mui/stack {:spacing 1 :sx {:padding 2}}
      [mui/stack {:direction :row :justify-content :space-between}
       [mui/typography {:variant :h5}
        (if (= type :add)
          "add other expense"
          "edit other expense")]
       [mui/icon-button {:on-click #(rf/dispatch [::funeral-model/hide-funeral-dialog])}
        [ui/icon-close]]]
      [other-form/form values
       (if (= type :add)
         #(rf/dispatch [::add-expense case-id %])
         (let [expense-id @(rf/subscribe [::funeral-model/dialog-info])]
           #(rf/dispatch [::edit-expense case-id expense-id %])))]])))
