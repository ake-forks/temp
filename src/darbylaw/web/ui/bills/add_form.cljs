(ns darbylaw.web.ui.bills.add-form
  (:require [darbylaw.web.util.form :as form-util]
            [darbylaw.web.ui.bills.model :as model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.case-model :as case-model]
            [vlad.core :as v]
            [darbylaw.web.util.vlad :as v+ :refer [v-some? v-when]]
            [clojure.edn :refer [read-string]]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.web.ui.bills.common :as common]))

(defn type-of-bill-choice [{:keys [values set-handle-change submitting?] :as _fork-args}]
  (let [all-bill-types @(rf/subscribe [::model/bill-types])
        checked-values (get values :bill-type #{})]
    [mui/form-control
     [mui/form-label "bill type (select all applicable)"]
     [mui/stack {:direction :row}
      (for [bill-types (partition 2 all-bill-types)]
        ^{:key (pr-str bill-types)}
        [mui/stack
         (for [{:keys [name label]} bill-types]
           ^{:key name}
           [mui/form-group
            [mui/form-control-label
             {:control (r/as-element
                         [mui/checkbox
                          {:checked (contains? checked-values name)
                           :onChange (fn [evt]
                                       (set-handle-change
                                         {:value (if (ui/event-target-checked evt)
                                                   (if (= name :council-tax)
                                                     #{:council-tax}
                                                     (-> checked-values
                                                       (conj name)
                                                       (disj :council-tax)))
                                                   (disj checked-values name))
                                          :path [:bill-type]}))
                           :name name
                           :disabled submitting?}])
              :label (if (contains? checked-values name)
                       (r/as-element [:b label])
                       label)}]])])]
     [mui/form-helper-text "helper text"]]))


(defn supplier-fields [{:keys [values handle-change] :as fork-args}]
  [:<>
   [mui/stack {:direction :row :spacing 1}
    [form-util/autocomplete-field fork-args
     {:name :supplier
      :full-width true
      :label "supplier"
      :value (if (:supplier-unknown values) "" (:supplier values))
      :disabled (:supplier-unknown values)
      :options (-> @(rf/subscribe [::model/all-company-ids])
                 (conj ""))
      :getOptionLabel @(rf/subscribe [::model/company-id->label])
      :inner-config {:required (not (:supplier-unknown values))
                     :disabled (:supplier-unknown values)}}]
    [mui/form-group
     [mui/form-control-label
      {:label "supplier not listed"
       :control
       (r/as-element [mui/switch {:name :supplier-unknown :on-change handle-change}])}]]]
   (if (:supplier-unknown values)
     [form-util/text-field fork-args
      {:name :supplier-new
       :hiddenLabel true
       :placeholder "enter supplier's company name"
       :multiline true
       :minRows 2
       :maxRows 3
       :variant :outlined
       :fullWidth true
       :required true}])])



(comment (defn issuer [{:keys [values handle-change set-handle-change] :as fork-args}]
           [:<>
            [mui/form-label
             "supplier"]
            [mui/form-control
             [mui/radio-group {:name :issuer-known
                               :value (get values :issuer-known)
                               :onChange handle-change
                               :sx {:gap (ui/theme-spacing 1)}}
              [mui/form-control-label
               {:value "known"
                :control (r/as-element
                           [mui/radio {:disabled (= issuer-type :unselected)}])
                :disableTypography true
                :label (r/as-element
                         (let [select! #(set-handle-change
                                          {:value "known"
                                           :path [:issuer-known]})]
                           [form-util/autocomplete-field fork-args
                            (merge
                              {:name :issuer
                               :sx {:flex-grow 1}
                               :label "supplier"
                               :onChange (fn [_evt new-value]
                                           (select!)
                                           (set-handle-change {:value new-value
                                                               :path [:issuer]}))}
                              {:options (-> @(rf/subscribe [::model/all-company-ids])
                                          (conj ""))
                               :getOptionLabel @(rf/subscribe [::model/company-id->label])})]))}]

              [mui/form-control-label
               {:value "custom"
                :control (r/as-element
                           [mui/radio {:disabled (= issuer-type :unselected)}])
                :sx {:align-items :flex-start}
                :disableTypography true
                :label (r/as-element
                         (let [disabled? (or (= issuer-type :unselected)
                                           (not= "custom" (get values :issuer-known)))
                               select! #(set-handle-change
                                          {:value "custom"
                                           :path [:issuer-known]})]
                           [mui/stack {:sx {:flex-grow 1}}
                            [mui/typography (merge
                                              {:sx {:p 1}}
                                              (when disabled?
                                                {:color :text.disabled}))
                             (str issuer-type-label " is not in the list, please enter")]
                            [mui/stack {:spacing 1}
                             [form-util/text-field fork-args
                              (merge
                                {:name :custom-issuer-name
                                 :label (str issuer-type-label " name")
                                 :disabled disabled?}
                                (when disabled?
                                  {:onClick select!}))]
                             [form-util/text-field fork-args
                              {:name :custom-issuer-address
                               :label (str issuer-type-label " address")
                               :disabled disabled?
                               :multiline true
                               :minRows 3
                               :maxRows 3}]]]))}]]]]))

(defn account-number-field [fork-args]
  [form-util/text-field fork-args {:name :account-number
                                   :label "account number"
                                   :required true}])

(defn property-field [{:keys [values handle-change set-handle-change] :as fork-args}]
  (let [error (form-util/get-error :property fork-args)]
    [mui/form-control {:error (boolean error)}
     [mui/form-label {:sx {:mt 1 :mb 2}}
      "property address"]
     [mui/radio-group {:name :property
                       ; `pr-str` and `read` used for distinguishing keywords from strings
                       :value (pr-str (get values :property))
                       :onChange (fn [_evt v]
                                   (set-handle-change
                                     {:value (read-string v)
                                      :path [:property]}))
                       :sx {:gap (ui/theme-spacing 1)}}
      (for [{property-id :id
             address :address} @(rf/subscribe [::model/current-properties])]
        [mui/form-control-label
         {:key (pr-str property-id)
          :value (pr-str property-id)
          :control (r/as-element [mui/radio])
          :sx {:align-items :flex-start}
          :disableTypography true
          :label (r/as-element
                   (let [selected? (= property-id (get values :property))]
                     [common/address-box selected? address]))}])
      (let [deceased-address @(rf/subscribe [::case-model/deceased-address])
            properties @(rf/subscribe [::model/current-properties])
            used-addresses (map :address properties)]
        (when (->> used-addresses
                (not-any? #(= (data-util/sanitize-empty-space %)
                             (data-util/sanitize-empty-space deceased-address))))
          [mui/form-control-label
           {:value (pr-str deceased-address)
            :control (r/as-element [mui/radio])
            :disableTypography true
            :label (r/as-element
                     (let [selected? (= deceased-address (get values :property))]
                       [common/address-box selected? deceased-address]))}]))
      [mui/form-control-label
       {:value (pr-str :new-property)
        :control (r/as-element [mui/radio])
        :sx {:align-items :flex-start}
        :disableTypography true
        :label (r/as-element
                 (let [selected? (= :new-property (get values :property))
                       select! #(set-handle-change
                                  {:value :new-property
                                   :path [:property]})]
                   [form-util/text-field fork-args
                    (merge
                      {:name :address-new
                       :hiddenLabel true
                       :placeholder "enter another address"
                       :multiline true
                       :minRows 3
                       :maxRows 5
                       :variant :outlined
                       :fullWidth true
                       :value (if selected?
                                (get values :address-new)
                                "")
                       :onChange (fn [evt]
                                   (select!)
                                   (handle-change evt))
                       :onClick #(select!)}
                      ; Doesn't work with multiline textfield. Not important.
                      (when-not selected?
                        {:inputProps {:tabindex :-1}}))]))}]]
     (when error
       [mui/form-helper-text (first error)])]))

(defn amount-field [fork-args]
  [form-util/text-field fork-args
   {:name :amount
    :label "amount"
    :full-width true
    :required true
    :InputProps
    {:start-adornment
     (r/as-element
       [mui/input-adornment {:position :start} "£"])}}])

(defn meter-readings-field [fork-args]
  [form-util/text-field fork-args {:name :meter-readings
                                   :label "meter readings"}])

(defn form [fork-args]
  [mui/stack {:spacing 2}
   [type-of-bill-choice fork-args]
   [mui/divider]
   [supplier-fields fork-args]
   [mui/divider]
   [property-field fork-args]
   [mui/divider]
   [mui/form-label "bill details"]
   [account-number-field fork-args]
   [amount-field fork-args]
   [meter-readings-field fork-args]])

(def validation
  (v/join
    (v/attr [:property] (v-some?))
    (v-when #(= :new-property (:property %))
      (v/attr [:address-new] (v/present)))
    (v-when #(= "custom" (:issuer-known %))
      (v/join
        (v/attr [:custom-issuer-name] (v/present))
        (v/attr [:custom-issuer-address] (v/present))))
    (v-when #(= "known" (:issuer-known %))
      (v/attr [:issuer] (v/present)))
    (v/attr [:amount] (v/chain
                        (v/present)
                        (v+/currency?)))))

(defn validate [form-data]
  (v/field-errors validation form-data))

(comment
  ;from values-to-submit
  (= "known" (:issuer-known values))
  (-> (update :issuer keyword)
    (dissoc :custom-issuer-name
      :custom-issuer-address))
  (= "custom" (:issuer-known values))
  (dissoc :issuer)
  :always
  (dissoc :issuer-known))