(ns darbylaw.web.ui.bills.account-info
  (:require
    [darbylaw.web.ui.bills.common :as common]
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [darbylaw.web.ui :as ui :refer [<<]]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.notification.model :as notification-model]
    [darbylaw.web.ui.bills.model :as bills-model]
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

(defn utility-item [{:keys [bill-type account-number id] :as data}]
  (let [type-string (interpose " & " bill-type)]
    [mui/box
     [mui/card
      [mui/stack {:direction :row :sx {:p 1} :justify-content :space-between}
       [mui/stack {:direction :row :spacing 2 :align-items :center}
        [mui/typography {:variant :h6} type-string]
        [mui/typography {:variant :body1} "account no: " account-number]]
       [mui/stack {:direction :row :spacing 2 :align-items :center}
        [mui/icon-button {:on-click #(rf/dispatch [::edit-utility data])}
         [ui/icon-edit]]
        [mui/icon-button {:on-click #(reset! popover {:anchor (ui/event-currentTarget %)
                                                      :label type-string
                                                      :asset-id id})}
         [ui/icon-delete]]]]
      [confirmation-popover]]]))

(defn recent-bill-component [])


(defn council-item [{:keys [account-number id recent-bill] :as data}]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        council-label (bill-data/get-council-label (:council (<< ::notification-model/notification)))]
    [mui/box
     [mui/card
      [mui/stack {:sx {:p 1}
                  :spacing 0.5}
       [mui/stack {:direction :row
                   :justify-content :space-between
                   :align-items :center}
        [mui/typography {:variant :body1} (str council-label)]
        [mui/stack {:direction :row :spacing 2 :align-items :center}
         [mui/icon-button {:on-click #(rf/dispatch [::edit-council data])}
          [ui/icon-edit]]
         [mui/icon-button {:on-click #(reset! popover {:anchor (ui/event-currentTarget %)
                                                       :label council-label
                                                       :asset-id id})}
          [ui/icon-delete]]]]
       (when account-number
         [mui/typography {:variant :body1} (str "account number: " account-number)])
       [mui/stack {:spacing 1 :direction :row :align-items :center}
        (if recent-bill
          [mui/stack {:direction :row :spacing 1 :align-items :center}
           [mui/typography {:variant :body1} "recent bill:"]
           [mui/button {:variant :text}
            (:original-filename recent-bill)]
           [common/upload-button
            :council-tax
            case-id
            id
            {:variant :outlined}
            "replace"]]
          [common/upload-button
           :council-tax
           case-id
           id
           {:variant :outlined}
           "upload recent bill?"])]]
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