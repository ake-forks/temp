(ns darbylaw.web.ui.bills.bills-dialog
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui.case-model :as case-model]
    [fork.re-frame :as fork]
    [darbylaw.web.ui.bills.add-form :as add-form]
    [darbylaw.web.ui.bills.model :as model]))

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ issuer-id]]
    (if (some? issuer-id)
      (merge db {::dialog-open? true
                 ::issuer-id issuer-id})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::form-submitting?
  (fn [db]
    (::form-submitting? db)))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (or (::dialog-open? db)
        (::form-submitting? db))))

(rf/reg-sub ::issuer-id
  (fn [db]
    (::issuer-id db)))

(def form-state (r/atom nil))

(defn set-submitting [db fork-params submitting?]
  (-> db
    (fork/set-submitting (:path fork-params) submitting?)
    (assoc ::form-submitting? submitting?)))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id fork-params _response]]
    {:db (-> db
           (set-submitting fork-params false)
           (assoc ::dialog-open? false))
     ; Should we wait until case is loaded to close the dialog?
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ fork-params error-result]]
    {:db (set-submitting db fork-params false)
     ::ui/notify-user-http-error {:message "Error on adding household bill"
                                  :result error-result}}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:db (set-submitting db fork-params true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bill")
        :params (add-form/values-to-submit fork-params)
        :on-success [::submit-success case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))

(defn form []
  #_[form-util/form
     {:state form-state
      :on-submit (let [case-id @(rf/subscribe [::case-model/case-id])]
                   #(rf/dispatch [::submit! case-id %]))
      :validation add-form/validate
      :initial-values add-form/initial-values}
     (fn [{:keys [handle-submit submitting?] :as fork-args}]
       [:form {:on-submit handle-submit}
        [mui/dialog-content
         [add-form/form fork-args]]
        [mui/dialog-actions
         [ui/loading-button {:type :submit
                             :loading submitting?
                             :variant :contained}
          "Add"]
         [mui/button {:onClick #(rf/dispatch [::set-dialog-open nil])
                      :disabled @(rf/subscribe [::form-submitting?])
                      :variant :outlined}
          "Cancel"]]])])

(rf/reg-sub ::issuer-label
  :<- [::model/company-id->label]
  :<- [::issuer-id]
  (fn [[company-id->label issuer-id]]
    (if (keyword? issuer-id)
      (company-id->label issuer-id)
      (:custom-issuer-name issuer-id))))

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))}
   [mui/dialog-title
    (str "household bills for " @(rf/subscribe [::issuer-label]))
    [mui/icon-button {:onClick #(rf/dispatch [::set-dialog-open nil])
                      :disabled @(rf/subscribe [::form-submitting?])}
     [ui/icon-close]]]
   [form]])

(defn show [issuer-id]
  (rf/dispatch [::set-dialog-open issuer-id]))
