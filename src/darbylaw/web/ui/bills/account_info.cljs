(ns darbylaw.web.ui.bills.account-info
  (:require
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.web.util.form :as form-util]
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [darbylaw.web.ui :as ui :refer [<<]]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.notification.model :as notification-model]
    [darbylaw.web.ui.bills.model :as bills-model]
    [darbylaw.web.ui.bills.add-form :as bills-form]
    [darbylaw.api.bill.data :as bill-data]
    [reagent.core :as r]))

; TODO what are we calling the bill / asset / account?
; need term for both council-tax and utility-bill

(defn current-utility-data [utility-company property]
  (let [current-case (<< ::case-model/current-case)
        bills-by-property-id (group-by :property (:utility-bills current-case))]
    (filter #(= utility-company (:utility-company %))
      (get bills-by-property-id (uuid (str property))))))

(defn current-council-data [council property]
  (let [current-case (<< ::case-model/current-case)
        councils-by-property-id (group-by :property (:council-tax current-case))]
    (filter #(= council (:council %))
      (get councils-by-property-id (uuid (str property))))))
(def popover (r/atom nil))

(rf/reg-event-fx ::delete-success
  (fn [_ [_ notification-type case-id _response]]
    (reset! popover nil)
    {:fx [(when (= notification-type :council-tax)
            [:dispatch [::bills-model/show-bills-dialog nil]]
            [:dispatch [::notification-model/close-dialog]])
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::delete-failure
  (fn [_ [_ error-result]]
    {::ui/notify-user-http-error {:message "Error deleting"
                                  :result error-result}}))
(rf/reg-event-fx ::delete!
  (fn [_ [_ notification-type case-id id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (case notification-type
               :utility (str "/api/case/" case-id "/delete-utility/" id)
               :council-tax (str "/api/case/" case-id "/delete-council-tax/" id))
        :on-success [::delete-success notification-type case-id]
        :on-failure [::delete-failure]})}))

(rf/reg-event-fx ::edit-utility
  (fn [_ [_ data]]
    {:fx [[:dispatch [::bills-model/save-temp-data {:values (assoc data :utility-company (name (:utility-company data)))}]]
          [:dispatch [::bills-model/show-bills-dialog
                      {:service :utility
                       :id (:utility-company data)
                       :dialog :edit}]]]}))

(rf/reg-event-fx ::edit-council
  (fn [_ [_ data]]
    {:fx [[:dispatch [::bills-model/save-temp-data {:values (assoc data :council (name (:council data)))}]]
          [:dispatch [::bills-model/show-bills-dialog
                      {:service :council-tax
                       :id (:council data)
                       :dialog :edit}]]]}))

(rf/reg-event-fx ::print!
  (fn [_ [_ fork-args]]
    (print fork-args)))
(def valuation-visible (r/atom false))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id fork-params _response]]
    (reset! valuation-visible false)
    {:db (-> db
           (model/set-submitting fork-params false))
     ; Should we wait until case is loaded to close the dialog?
     :fx [
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ fork-params error-result]]
    (print error-result)
    {:db (model/set-submitting db fork-params false)
     ::ui/notify-user-http-error {:message "Error on adding household bill"
                                  :result error-result}}))

(rf/reg-event-fx ::submit-valuation
  (fn [{:keys [db]} [_  case-id existing-id fork-params]]
    {:db (model/set-submitting db fork-params true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/submit-valuation/" existing-id)
        :params (:values fork-params)
        :on-success [::submit-success case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))


(defonce form-state (r/atom nil))
(defn valuation-form [{:keys [account-number id meter-readings valuation]}]
  (let [case-id (<< ::case-model/case-id)]
    [form-util/form
     {:state form-state
      :on-submit #(rf/dispatch [::submit-valuation case-id id %])
      :initial-values {:account-number (or account-number "")
                       :meter-readings (or meter-readings "")
                       :valuation (or valuation "")}}
     (fn [{:keys [values handle-submit submitting?] :as fork-args}]
       [:form {:on-submit handle-submit}
        [mui/stack {:spacing 0.5}
         [mui/stack {:direction :row :spacing 0.5}
          [bills-form/account-number-field fork-args]
          [bills-form/meter-readings-field fork-args]]
         [mui/stack {:direction :row :spacing 0.5}
          [bills-form/valuation-field fork-args]]
         [mui/button {:type :submit :variant :outlined} "save"]]])]))



(defn confirmation-popover []
  (let [case-id (<< ::case-model/case-id)
        notification-type (<< ::notification-model/notification-type)]
    [mui/popover {:open (not (nil? @popover))
                  :anchor-el (get @popover :anchor)
                  :on-close #(reset! popover nil)
                  :anchor-origin {:vertical "bottom" :horizontal "right"}
                  :transform-origin {:vertical "top" :horizontal "right"}}
     [mui/stack {:sx {:p 1}}
      ;TODO if it's the last account for that supplier, say 'removing this account will remove Supplier'
      [mui/typography "Are you sure you want to remove the details"]
      [mui/typography " for this " (get @popover :label) " account?"]
      [mui/stack {:direction :row
                  :sx {:mt 1}
                  :justify-content :space-between}
       [mui/button {:variant :outlined
                    :color :error
                    :full-width true
                    :on-click #(rf/dispatch [::delete! notification-type case-id (get @popover :id)])} "yes, remove account"]]]]))

(defn utility-item [{:keys [bill-type account-number id valuation] :as data}]
  (let [type-string (interpose " & " bill-type)
        ongoing? (<< ::notification-model/notification-ongoing?)]
    [mui/box
     [mui/card
      [mui/stack {:direction :row :sx {:p 1} :justify-content :space-between}
       [mui/stack {:direction :row :spacing 2 :align-items :center}
        [mui/typography {:variant :h6} type-string]
        [mui/typography {:variant :body1} "account no: " account-number]]
       [mui/stack {:direction :row :spacing 2 :align-items :center}
        (when ongoing?
          [mui/tooltip {:title "add valuation"}
           [mui/icon-button {:on-click #(reset! valuation-visible (not @valuation-visible))}
            [ui/icon-pound]]])
        [mui/tooltip {:title "edit"}
         [mui/icon-button {:on-click #(rf/dispatch [::edit-utility data])}
          [ui/icon-edit]]]
        [mui/tooltip {:title "remove"}
         [mui/icon-button {:on-click #(reset! popover {:anchor (ui/event-currentTarget %)
                                                       :label type-string
                                                       :id id})}
          [ui/icon-delete]]]]]
      (when valuation
        [mui/typography {:variant :h6} "account value: £" valuation])]

     (when @valuation-visible
       [valuation-form data])
     [confirmation-popover]]))


(defn council-item [{:keys [account-number id] :as data}]
  (let [council-label (bill-data/get-council-label (:council (<< ::notification-model/notification)))]
    [mui/box
     [mui/card
      [mui/stack {:direction :row :sx {:p 1}
                  :justify-content :space-between
                  :align-items :center}
       [mui/typography {:variant :body1} (str council-label " account"
                                           (when account-number
                                             (str ": " account-number)))]
       [mui/stack {:direction :row :spacing 2 :align-items :center}
        [mui/icon-button {:on-click #(rf/dispatch [::edit-council data])}
         [ui/icon-edit]]
        [mui/icon-button {:on-click #(reset! popover {:anchor (ui/event-currentTarget %)
                                                      :label council-label
                                                      :id id})}
         [ui/icon-delete]]]]
      [confirmation-popover]]]))

(defn utility-bill-info []
  (let [notification-data (<< ::notification-model/notification)
        data (current-utility-data (:utility-company notification-data) (:property notification-data))]
    [mui/stack {:spacing 1}
     (map (fn [account]
            ^{:key (:id account)}
            [utility-item account])
       data)]))

(defn council-tax-info []
  (let [notification-data (<< ::notification-model/notification)
        data (current-council-data (:council notification-data) (:property notification-data))]
    [mui/stack {:spacing 1}
     (map (fn [account]
            ^{:key (:id account)}
            [council-item account])
       data)]))