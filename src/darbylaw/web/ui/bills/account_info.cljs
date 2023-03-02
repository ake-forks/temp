(ns darbylaw.web.ui.bills.account-info
  (:require
    [darbylaw.web.ui.bills.common :as common]
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
  (fn [_ [_ notification-type case-id asset-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (case notification-type
               :utility (str "/api/case/" case-id "/delete-utility/" asset-id)
               :council-tax (str "/api/case/" case-id "/delete-council-tax/" asset-id))
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
                       :council-id (:council data)
                       :dialog :edit}]]]}))
(def valuation-visible (r/atom false))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id fork-params _response]]
    (reset! valuation-visible false)
    {:db (-> db
           (bills-model/set-submitting fork-params false))
     ; Should we wait until case is loaded to close the dialog?
     :fx [
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ fork-params error-result]]
    (print error-result)
    {:db (bills-model/set-submitting db fork-params false)
     ::ui/notify-user-http-error {:message "Error on adding household bill"
                                  :result error-result}}))

(rf/reg-event-fx ::submit-valuation
  (fn [{:keys [db]} [_ asset-type case-id existing-id fork-params]]
    {:db (bills-model/set-submitting db fork-params true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri
        (case asset-type
          :utility (str "/api/case/" case-id "/update-utility/" existing-id)
          :council-tax (str "/api/case/" case-id "/update-council-tax/" existing-id))
        :params (:values fork-params)
        :on-success [::submit-success case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))

(rf/reg-event-fx ::open-document
  (fn [_ [_ case-id filename]]
    (js/window.open
      (str "/api/case/" case-id "/household-bills/document/" filename))))

(defonce form-state (r/atom nil))
(defn valuation-form [asset-type {:keys [account-number id meter-readings valuation property]}]
  (let [case-id (<< ::case-model/case-id)]
    [form-util/form
     {:state form-state
      :on-submit #(rf/dispatch [::submit-valuation asset-type case-id id %])
      :initial-values {:account-number (or account-number "")
                       :valuation (or valuation "")
                       :property property
                       (if (= asset-type :utility)
                         :meter-readings) (or meter-readings "")}}
     (fn [{:keys [handle-submit] :as fork-args}]
       [:form {:on-submit handle-submit}
        [mui/stack {:spacing 0.5}
         [mui/stack {:direction :row :spacing 0.5}
          [bills-form/account-number-field fork-args]
          (if (= asset-type :utility)
            [bills-form/meter-readings-field fork-args])]
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
                    :on-click #(rf/dispatch [::delete! notification-type case-id (get @popover :asset-id)])} "yes, remove account"]]]]))

(defn get-latest-bill [recent-bills]
  (first (sort-by :uploaded-at #(> %1 %2) recent-bills)))

(defn recent-bill-component [bill-type {:keys [id recent-bill]}]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        latest-bill (get-latest-bill recent-bill)]
    [mui/stack {:spacing 1 :direction :row :align-items :center}
     (if latest-bill
       [mui/stack {:direction :row :spacing 1 :align-items :center}
        [mui/typography {:variant :body1} "recent bill:"]
        [mui/button {:variant :text
                     :on-click #(rf/dispatch [::open-document case-id (:filename latest-bill)])}
         (:original-filename latest-bill)]
        [common/upload-button
         bill-type
         case-id
         id
         {:variant :outlined}
         "replace"]]
       [common/upload-button
        bill-type
        case-id
        id
        {:variant :outlined}
        "upload recent bill?"])]))

(defn utility-item [{:keys [bill-type account-number id valuation] :as data}]
  (let [type-string (interpose " & " bill-type)
        ongoing? (<< ::notification-model/notification-ongoing?)]
    [mui/box
     [mui/card
      [mui/stack {:sx {:p 1}
                  :spacing 0.5}
       [mui/stack {:direction :row
                   :justify-content :space-between
                   :align-items :center}
        [mui/typography {:variant :h6} type-string]
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
                                                        :asset-id id})}
           [ui/icon-delete]]]]]
       (when (not (clojure.string/blank? account-number))
         [mui/typography {:variant :body1} (str "account number: " account-number)])
       (when valuation
         [mui/typography {:variant :h6} "account value: £" valuation])
       [recent-bill-component :utility data]]
      (when @valuation-visible
        [valuation-form :utility data])
      [confirmation-popover]]]))


(defn council-item [{:keys [account-number id valuation] :as data}]
  (let [council-label (bill-data/get-council-label (:council (<< ::notification-model/notification)))
        ongoing? true #_(<< ::notification-model/notification-ongoing?)]
    [mui/box
     [mui/card
      [mui/stack {:sx {:p 1}
                  :spacing 0.5}
       [mui/stack {:direction :row
                   :justify-content :space-between
                   :align-items :center}
        [mui/typography {:variant :body1} (str council-label)]
        [mui/stack {:direction :row :spacing 2 :align-items :center}
         (when ongoing?
           [mui/tooltip {:title "add valuation"}
            [mui/icon-button {:on-click #(reset! valuation-visible (not @valuation-visible))}
             [ui/icon-pound]]])
         [mui/tooltip {:title "edit"}
          [mui/icon-button {:on-click #(rf/dispatch [::edit-council data])}
           [ui/icon-edit]]]
         [mui/tooltip {:title "remove"}
          [mui/icon-button {:on-click #(reset! popover {:anchor (ui/event-currentTarget %)
                                                        :label council-label
                                                        :asset-id id})}
           [ui/icon-delete]]]]]
       [mui/typography {:variant :body1} (if (not (clojure.string/blank? account-number))
                                           (str "council tax account no: " account-number)
                                           "council tax account")]
       (when (and valuation ongoing?)
         [mui/typography {:variant :body1} (str "account value: £" valuation)])
       [recent-bill-component :council-tax data]]

      (when @valuation-visible
        [valuation-form :council-tax data])
      [confirmation-popover]]]))

(defn utility-bill-info []
  (let [notification-data (<< ::notification-model/notification)
        data (bills-model/current-utility-data (:utility-company notification-data) (:property notification-data))]
    [mui/stack {:spacing 1}
     (map (fn [account]
            ^{:key (:id account)}
            [utility-item account])
       data)]))

(defn council-tax-info []
  (let [notification-data (<< ::notification-model/notification)
        data (bills-model/current-council-data (:council notification-data) (:property notification-data))]
    [mui/stack {:spacing 1}
     (map (fn [account]
            ^{:key (:id account)}
            [council-item account])
       data)]))