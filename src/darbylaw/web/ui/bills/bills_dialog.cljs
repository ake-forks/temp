(ns darbylaw.web.ui.bills.bills-dialog
  (:require
    [clojure.string :as str]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.bills.common :as common]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [fork.re-frame :as fork]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.api.bill.data :as bill-data]))

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

(def form-state (r/atom nil))

(defn set-submitting [db fork-params submitting?]
  (-> db
    (fork/set-submitting (:path fork-params) submitting?)
    (assoc ::form-submitting? submitting?)))

(rf/reg-sub ::issuer-label
  :<- [::model/company-id->label]
  :<- [::dialog-context]
  (fn [[company-id->label {:keys [issuer-id]}]]
    (if (keyword? issuer-id)
      (company-id->label issuer-id)
      issuer-id)))

(rf/reg-sub ::dialog-property
  :<- [::dialog-context]
  :<- [::model/current-properties-by-id]
  (fn [[context properties-by-id]]
    (get properties-by-id (:property-id context))))

(rf/reg-sub ::property-address
 :<- [::dialog-property]
  (fn [property]
    (:address property)))

(rf/reg-sub ::dialog-bills
  :<- [::model/current-bills]
  :<- [::dialog-context]
  (fn [[all-bills {:keys [issuer-id property-id]}]]
    (->> all-bills
      (filter (fn [bill]
                (and (= issuer-id (:issuer bill))
                     (= property-id (:property bill))))))))

(defn concepts-str [concepts]
  (->> bill-data/bill-types
    (keep (fn [[k {:keys [label]}]]
            (when (contains? concepts k)
              label)))
    (str/join "&")))

(defn bill-row [bill]
  [mui/card {:elevation 2}
   [mui/card-content
    [mui/stack {:direction :row
                :spacing 2
                :sx {:align-items :center}}
     [mui/stack {:sx {:flex-grow 1}}
      [mui/typography {:variant :subtitle} [:b (concepts-str (:bill-type bill))]]
      (when-let [account-number (:account-number bill)]
        [mui/typography {:variant :subtitle2} "account: " account-number])]
     ;[mui/divider {:orientation :vertical :flexItem true}]
     [mui/typography {:variant :h6} (str "£" (:amount bill))]
     ;[mui/divider {:orientation :vertical :flexItem true}]
     [mui/box
      [mui/icon-button
       [ui/icon-edit]]
      [mui/icon-button
       [ui/icon-delete]]]]]])

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))}
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
         [bill-row bill])]]]]
   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :onClick #(rf/dispatch [::set-dialog-open nil])}
     "Close"]]])

(defn show {:arglists '({:keys [issuer-id property-id]})}
  [dialog-context]
  (rf/dispatch [::set-dialog-open dialog-context]))
