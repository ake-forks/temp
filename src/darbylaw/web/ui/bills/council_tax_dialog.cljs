(ns darbylaw.web.ui.bills.council-tax-dialog
  (:require
    [darbylaw.web.ui :as ui :refer [<<]]
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.notification.model :as notification-model]
    [darbylaw.web.ui.bills.common :as common]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.bills.model :as model]
    [reagent.core :as r]))

(defonce form-state (r/atom nil))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id fork-params response]]
    {:db (-> db
           (model/set-submitting fork-params false))
     ; Should we wait until case is loaded to close the dialog?
     :fx [[:dispatch [::model/show-bills-dialog nil]]
          [:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::notification-model/open
                      {:notification-type :council-tax
                       :case-id case-id
                       :council (:council response)
                       :property (:property response)
                       :asset-id (:id response)}]]]}))


(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ fork-params error-result]]
    {:db (model/set-submitting db fork-params false)
     ::ui/notify-user-http-error {:message "Error on adding household bill"
                                  :result error-result}}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ dialog-type case-id fork-params existing-id]]
    {:db (model/set-submitting db fork-params true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri
        (case dialog-type
          :add (str "/api/case/" case-id "/council-tax")
          :edit (str "/api/case/" case-id "/update-council-tax/" existing-id))
        :params (model/values-to-submit fork-params)
        :on-success [::submit-success case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))

(defn council-select [fork-args dialog-type]
  [form-util/autocomplete-field fork-args
   {:name :council
    :label "council"
    :options (-> @(rf/subscribe [::model/all-council-ids])
               (conj ""))
    :getOptionLabel @(rf/subscribe [::model/council-id->label])
    :inner-config {:required true
                   :disabled (= dialog-type :edit)}}])

(defn layout [{:keys [handle-submit submitting?] :as fork-args} dialog-type]
  (let [relationship @(rf/subscribe [::case-model/relationship])]
    [:form {:on-submit handle-submit}
     [mui/dialog-content {:style {:height "50vh"
                                  :width "100%"
                                  :padding "1rem"}}
      [mui/stack {:spacing 1 :sx {:height 1 :width 1}}
       [council-select fork-args dialog-type]
       [mui/divider]
       [common/property-select fork-args dialog-type]
       [common/new-property-input fork-args]
       [mui/divider]
       [mui/typography {:variant :body1}
        (str "Enter your late " relationship "'s council tax account number or reference if known.
        This can usually be found at the top of a council tax bill.")]
       [form-util/text-field fork-args
        {:name :account-number
         :label "account number"}]]]
     [mui/dialog-actions
      [mui/dialog-actions
       [mui/stack {:direction :row :spacing 1}
        [mui/button {:onClick #(do (rf/dispatch [::model/clear-temp-data])
                                   (rf/dispatch [::model/show-bills-dialog nil]))
                     :disabled @(rf/subscribe [::model/form-submitting?])
                     :variant :outlined}
         "Cancel"]
        [ui/loading-button {:onClick handle-submit
                            :loading submitting?
                            :variant :contained}
         "Save"]]]]]))

(defn panel []
  (let [dialog @(rf/subscribe [::model/bills-dialog])
        dialog-type (:dialog dialog)
        case-id @(rf/subscribe [::case-model/case-id])
        temp-data (rf/subscribe [::model/get-temp-data])]
    [mui/dialog {:open (= :council-tax (:service dialog))
                 :maxWidth :md
                 :full-width true
                 :scroll :paper}
     [mui/dialog-title
      [mui/stack {:direction :row :justify-content :space-between}
       [mui/typography {:variant :h4}
        (case dialog-type
          :add "add council tax"
          :edit "edit council tax"
          "")]
       [mui/icon-button {:onClick #(rf/dispatch [::model/show-bills-dialog nil])}
        [ui/icon-close]]]]

     (if (= dialog-type :edit)
       [common/upload-button
        :council-tax
        case-id
        (:id @temp-data)
        {:full-width false}
        "upload recent bill"])
     [form-util/form
      {:state form-state
       :on-submit #(rf/dispatch [::submit!
                                 dialog-type
                                 case-id
                                 %
                                 (when temp-data (:id @temp-data))])
       :initial-values (when temp-data @temp-data)}
      (fn [fork-args]
        [layout fork-args dialog-type])]]))