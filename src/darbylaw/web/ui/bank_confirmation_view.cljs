(ns darbylaw.web.ui.bank-confirmation-view
  (:require [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.bank-model :as bank-model]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-add :as bank-add]
            [darbylaw.api.bank-list :as bank-list]
            [darbylaw.web.util.form :as form]
            [fork.re-frame :as fork]
            [reagent.core :as r]
            [kee-frame.core :as kf]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.styles :as styles]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.bank-add :as bank]))


(rf/reg-event-fx ::update-bank-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (print response)
    {:db (fork/set-submitting db path false)}
    (rf/dispatch [::bank-model/hide-bank-dialog])))

(rf/reg-event-fx ::update-bank-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::update-bank
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/bank-api/" case-id "/update-bank-accounts")
        :params (bank/transform-on-submit values)
        :on-success [::update-bank-success fork-params]
        :on-failure [::update-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:dispatch [::update-bank case-id fork-params]}))


(defn bank-confirmation-form [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/stack {:spacing 2}
    [fork/field-array {:props fork-args
                       :name :accounts}
     bank-add/account-array-fn]]
   [mui/stack {:direction :row :spacing 1}
    [mui/button {:on-click #(rf/dispatch [::bank-model/hide-bank-dialog]) :variant :contained :full-width true} "cancel"]
    [mui/button {:type :submit :variant :contained :full-width true} "submit"]]])


(defonce form-state (r/atom nil))

(defn bank-confirmation-panel []
  (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        current-case @(rf/subscribe [::case-model/current-case])
        banks (-> @(rf/subscribe [::case-model/current-case]) :bank-accounts)
        current-bank (filter #(= (:id %) bank-id) banks)
        accounts (:accounts (first current-bank))]
    (rf/dispatch [::case-model/load-case! case-id])
    [mui/box
     [mui/stack {:style {:min-height "inherit"}
                 :spacing 1 :direction :row
                 :justify-content :space-between
                 :divider (r/as-element [mui/divider {:style {:border-color theme/rich-black
                                                              :border-width "1px"}}])}
      ;left-hand side
      [mui/box
       [mui/stack {:spacing 1}
        [mui/typography {:variant :h5}
         (str "confirm your "
           (-> current-case :deceased :relationship)
           "'s accounts with " (bank-list/bank-label bank-id))]
        [mui/typography {:variant :p} (str "We have received a letter of valuation from "
                                        (bank-list/bank-label bank-id) ". Please enter the confirmed value for each of the accounts listed,
                                        and add any additional accounts mentioned in the letter.")]
        (if (some? accounts)
          (r/with-let []
            [fork/form
             {:state form-state
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit! case-id %])
              :keywordize-keys true
              :prevent-default? true
              :disable :estimated-value
              :initial-values {:accounts accounts :bank-id (name bank-id)}}
             (fn [fork-args]
               [mui/box
                [bank-confirmation-form (ui/mui-fork-args fork-args)]])]
            (finally
              (reset! form-state nil))))]]]]))

