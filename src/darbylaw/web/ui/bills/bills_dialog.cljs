(ns darbylaw.web.ui.bills.bills-dialog
  (:require
    [clojure.string :as str]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.bills.common :as common]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.api.bill.data :as bill-data]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::dialog-open? true
                 ::dialog-context dialog-context})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::form-submitting?
  (fn [db]
    (::form-submitting? db)))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (or (::dialog-open? db)
        (::form-submitting? db))))

(rf/reg-sub ::dialog-context
  (fn [db]
    (::dialog-context db)))

(rf/reg-sub ::notification-process
  :<- [::case-model/current-case]
  :<- [::dialog-context]
  (fn [[case-data dialog-context]]
    (->> (:notification-process case-data)
      (filter #(= dialog-context (select-keys % [:asset-type
                                                 :utility-company
                                                 :property])))
      first)))

(rf/reg-sub ::notification-ongoing
  :<- [::notification-process]
  (fn [process]
    (:ready-to-start process)))


(def form-state (r/atom nil))

(defn set-submitting [db submitting?]
  (assoc db ::form-submitting? submitting?))

(rf/reg-event-fx ::start-notification-process-success
  (fn [{:keys [db]} [_ case-id _response]]
    {:db (-> db
           (set-submitting false)
           (assoc ::dialog-open? false))
     ; Should we wait until case is loaded to close the dialog?
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::start-notification-process-failure
  (fn [{:keys [db]} [_ error-result]]
    {:db (set-submitting db false)
     ::ui/notify-user-http-error {:message "Error on creating notification letter"
                                  :result error-result}}))

(rf/reg-event-fx ::start-notification-process
  (fn [{:keys [db]} [_ case-id context]]
    {:db (set-submitting db true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/start-notification-process")
        :params (merge {:asset-type :utility-bill}
                       context)
        :on-success [::start-notification-process-success case-id]
        :on-failure [::start-notification-process-failure]})}))

(rf/reg-sub ::issuer-label
  :<- [::model/company-id->label]
  :<- [::dialog-context]
  (fn [[company-id->label {:keys [utility-company]}]]
    (if (keyword? utility-company)
      (company-id->label utility-company)
      utility-company)))

(rf/reg-sub ::dialog-property
  :<- [::dialog-context]
  :<- [::model/current-properties-by-id]
  (fn [[context properties-by-id]]
    (get properties-by-id (:property context))))

(rf/reg-sub ::property-address
 :<- [::dialog-property]
  (fn [property]
    (:address property)))

(rf/reg-sub ::dialog-bills
  :<- [::model/current-bills]
  :<- [::dialog-context]
  (fn [[all-bills {:keys [utility-company property]}]]
    (->> all-bills
      (filter (fn [bill]
                (and (= utility-company (:issuer bill))
                     (= property (:property bill))))))))

(defn bill-types-str [bill-types]
  (->> bill-data/bill-types
    (keep (fn [[k {:keys [label]}]]
            (when (contains? bill-types k)
              label)))
    (str/join " & ")))

(defn bill-row [bill]
  [mui/card {:elevation 2}
   [mui/card-content
    [mui/stack {:direction :row
                :spacing 2
                :sx {:align-items :center}}
     [mui/stack {:sx {:flex-grow 1}}
      [mui/typography {:variant :subtitle} [:b (bill-types-str (:bill-type bill))]]
      (when-let [account-number (:account-number bill)]
        [mui/typography {:variant :subtitle2} "account: " account-number])]
     [mui/typography {:variant :h6} (str "£" (:amount bill))]
     [mui/box
      [mui/icon-button
       [ui/icon-edit]]
      [mui/icon-button
       [ui/icon-delete]]]]]])

(def anchor (r/atom nil))

(defn create-menu []
  [mui/menu {:open (some? @anchor)
             :anchor-el @anchor
             :on-close #(reset! anchor nil)
             :anchor-origin {:vertical :bottom
                             :horizontal :right}
             :transform-origin {:vertical :top
                                :horizontal :right}
             :PaperProps {:sx {:border-radius 0}}}
   [mui/menu-item {:on-click #(reset! anchor nil)}
    [mui/list-item-text
     (str "create notification letter")]]
   [mui/menu-item {:on-click #(reset! anchor nil)}
    [mui/list-item-text
     (str "upload received letter")]]])

(defn conversation []
  [mui/stack {:sx {:height "100%"}}
   [mui/dialog-title
    [mui/stack {:direction :row
                :sx {:justify-content :space-between}}
     "conversation"
     [mui/button {:startIcon (r/as-element [ui/icon-add])
                  :variant :contained
                  :onClick #(reset! anchor (ui/event-currentTarget %))}
      "new"]
     [create-menu]]]
   [mui/dialog-content
    [mui/paper {:sx {:overflow :auto
                     :height "100%"
                     :background-color :grey.400
                     :border-radius 0}}
     [mui/list
      [mui/list-item
       [mui/list-item-text "This is an item"]]]]]])

(def completed (r/atom false))

(defn dialog-content []
  [:<>
   [mui/dialog-title
    (str "household bills for " @(rf/subscribe [::issuer-label]))
    [mui/icon-button {:onClick #(rf/dispatch [::set-dialog-open nil])
                      :disabled @(rf/subscribe [::form-submitting?])}
     [ui/icon-close]]]
   [mui/dialog-content
    [mui/stack {:spacing 2}
     [mui/stack
      [mui/typography
       "For property at address"]
      [mui/stack
       [common/address-box false
        (let [address @(rf/subscribe [::property-address])]
          address)]]]
     [mui/stack
      [mui/typography {:sx {:font-weight 600}}
       "Bills"]
      [mui/stack {:spacing 2}
       (for [bill @(rf/subscribe [::dialog-bills])]
         ^{:key (:id bill)}
         [bill-row bill])]]
     (let [notification-ongoing? @(rf/subscribe [::notification-ongoing])]
       (when-not notification-ongoing?
         [mui/stack
          [mui/typography {:sx {:font-weight 600}}
           "Finished?"]
          [mui/typography
           "Let us know when you have provided all bill data at this address"
           " for company \"" @(rf/subscribe [::issuer-label]) "\". At that point,"
           " we will notify the company about the decease and ask for confirmation"
           " of the data entered."]
          [mui/form-control-label
           {:label "I have completed all bill data."
            :control
            (r/as-element
              [mui/checkbox {:checked @completed
                             :onChange #(reset! completed (ui/event-target-checked %))}])}]]))]]
   [mui/dialog-actions
    [mui/fade {:in @completed}
     [mui/button {:variant :contained
                  :onClick (let [case-id @(rf/subscribe [::case-model/case-id])
                                 context @(rf/subscribe [::dialog-context])]
                             #(rf/dispatch [::start-notification-process case-id context]))
                  :sx {:visibility (if @completed :visible :hidden)}}
      "Notify company"]]
    [mui/button {:variant :outlined
                 :onClick #(rf/dispatch [::set-dialog-open nil])}
     "Close"]]])

(defn dialog-content* []
  ; Dialog content separated to its own component, for running finally when unmounted.
  (r/with-let []
    (dialog-content)
    (finally
      (reset! completed false)
      (reset! anchor nil))))

(defn dialog []
  (let [notification-ongoing? @(rf/subscribe [::notification-ongoing])]
    [mui/dialog (merge
                  {:open (boolean @(rf/subscribe [::dialog-open?]))
                   :maxWidth (if notification-ongoing? :xl :sm)
                   :fullWidth true})
     [mui/stack {:spacing 1
                 :direction :row
                 :sx {:height "95vh"}}
      [mui/collapse (-> {:in notification-ongoing?
                         :orientation :horizontal
                         :spacing 1
                         :sx {:flex-grow 1}}
                      (ui/make-collapse-contents-full-width))
       [conversation]]
      [mui/stack {:sx {:width 620}}
       [dialog-content*]]]]))

(defn show {:arglists '({:keys [utility-company property]})}
  [dialog-context]
  (rf/dispatch [::set-dialog-open dialog-context]))
