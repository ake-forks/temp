(ns darbylaw.web.ui.bills.add-dialog
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.bills.add-form :as form]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.web.ui.bills.common :as common]
    [darbylaw.api.bill.data :as bill-data]))


(def form-state (r/atom nil))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id fork-params _response]]
    {:db (-> db
           (model/set-submitting fork-params false))
     ; Should we wait until case is loaded to close the dialog?
     :fx [[:dispatch [::model/show-bills-dialog nil]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ fork-params error-result]]
    {:db (model/set-submitting db fork-params false)
     ::ui/notify-user-http-error {:message "Error on adding household bill"
                                  :result error-result}}))

(rf/reg-event-fx ::submit-old!
  (fn [{:keys [db]} [_ case-id fork-params]]
    {:db (model/set-submitting db fork-params true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bill")
        :params (model/values-to-submit fork-params)
        :on-success [::submit-success case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [_]} [_ fork-params]]
    (print (model/values-to-submit fork-params))))

(defn second-layout [{:keys [values handle-submit submitting?] :as fork-args}]
  (let [supplier-data (bill-data/get-company-info (keyword (:supplier values)))]
    [:form {:on-submit handle-submit}
     [mui/dialog-content {:style {:height "50vh"
                                  :width "50vw"
                                  :padding "1rem"}}
      [mui/stack {:spacing 2 :sx {:height 1 :width 1}}]] [mui/stack {:direction :row :spacing 2}
                                                          [mui/box
                                                           [mui/typography {:variant :h6} "supply address"]
                                                           [common/address-box false
                                                            (case (:property values)
                                                              :deceased @(rf/subscribe [::case-model/deceased-address])
                                                              :new-property (:address-new values)
                                                              (:property values))]]
                                                          [mui/box
                                                           [mui/typography {:variant :h6} "supplier"]
                                                           [mui/typography {:variant :h5} (:common-name supplier-data)]]
                                                          [mui/divider]
                                                          [mui/typography {:variant :body1}
                                                           (str "The account number will be found on all correspondence from "
                                                             (:common-name supplier-data)
                                                             ". If you have a recent meter reading, entering that information below can speed up the next steps.")]
                                                          [mui/stack {:direction :row :spacing 2}
                                                           [form/type-of-bill-choice fork-args]
                                                           [mui/divider {:orientation :vertical}]
                                                           [mui/stack {:spacing 1}
                                                            [form/account-number-field fork-args]
                                                            [form/meter-readings-field fork-args]]]]
     [mui/dialog-actions
      [mui/button {:onClick #(do (rf/dispatch [::model/clear-temp-data]) (rf/dispatch [::model/show-bills-dialog nil]))
                   :disabled @(rf/subscribe [::model/form-submitting?])
                   :variant :outlined}
       "Cancel"]
      [ui/loading-button {:onClick handle-submit
                          :type :submit
                          :loading submitting?
                          :variant :contained}
       "Save"]]]))

(defn first-layout [{:keys [handle-submit submitting?] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/dialog-content {:style {:height "50vh"
                                :width "50vw"
                                :padding "1rem"}}
    [mui/stack {:spacing 1 :sx {:height 1 :width 1}}


     [form/supplier-fields fork-args]
     [mui/divider]
     [common/property-select fork-args]
     [common/new-property-input fork-args]]]
   [mui/dialog-actions
    [mui/button {:onClick #(do (rf/dispatch [::model/clear-temp-data]) (rf/dispatch [::model/show-bills-dialog nil]))
                 :disabled @(rf/subscribe [::model/form-submitting?])
                 :variant :outlined}
     "Cancel"]
    [ui/loading-button {:onClick #(rf/dispatch [::model/save-temp-data fork-args])
                        :loading submitting?
                        :variant :contained}
     "Next"]]])

(defn panel []
  [mui/dialog {:open (= :utility (:service @(rf/subscribe [::model/bills-dialog])))
               :maxWidth false
               :scroll :paper}
   [mui/dialog-title
    [mui/stack {:direction :row :justify-content :space-between}
     [mui/typography {:variant :h4} "add utility"]
     [mui/icon-button {:onClick #(rf/dispatch [::model/close-clear-dialog])}
      [ui/icon-close]]]]
   (let [temp-data (rf/subscribe [::model/get-temp-data])]
     [form-util/form
      {:state form-state
       :on-submit #(rf/dispatch [::submit! %])
       :initial-values (when temp-data @temp-data)}
      (fn [fork-args]
        (if (some? @temp-data)
          [second-layout fork-args]
          [first-layout fork-args]))])])

(defn show []
  (rf/dispatch [::set-dialog-open true]))
