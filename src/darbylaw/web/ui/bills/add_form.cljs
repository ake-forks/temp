(ns darbylaw.web.ui.bills.add-form
  (:require [darbylaw.web.util.form :as form-util]
            [darbylaw.web.ui.bills.model :as model]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.case-model :as case-model]
            [vlad.core :as v]
            [darbylaw.web.util.vlad :as v+]
            [clojure.edn :refer [read-string]]
            [clojure.string :refer [trim]]))

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

(defn company-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :company
    :label "company"
    :options @(rf/subscribe [::model/all-company-ids])
    :getOptionLabel @(rf/subscribe [::model/company-id->label])
    :freeSolo true}])

(defn council-select [fork-args]
  [form-util/autocomplete-field fork-args
   {:name :council
    :label "council"
    :options @(rf/subscribe [::model/all-council-ids])
    :getOptionLabel @(rf/subscribe [::model/council-id->label])
    :freeSolo true}])

(defn disabled-select-placeholder []
  [mui/text-field {:disabled true}])

(defn account-number-field [fork-args]
  [form-util/text-field fork-args {:name :account-number
                                   :label "account number"}])

(defn address-box [selected? child]
  [mui/paper (merge
               {:variant :outlined
                :sx (merge
                      {:flex-grow 1
                       :border-width 2
                       :padding 1
                       :white-space :pre}
                      (when selected?
                        {:border-color ui/theme-primary-light-color}))})
   child])

(defn address-field [{:keys [values handle-change set-handle-change] :as fork-args}]
  [mui/form-control
   [mui/form-label "supply address"]
   [mui/radio-group {:name :address
                     ; value is printed and read, for to distinguish keywords from strings
                     :value (pr-str (get values :address))
                     :onChange (fn [_evt v]
                                 (set-handle-change
                                   {:value (read-string v)
                                    :path [:address]}))
                     :sx {:gap (ui/theme-spacing 1)}}
    (let [deceased-address @(rf/subscribe [::case-model/deceased-address])]
      [mui/form-control-label
       {:value (pr-str :deceased-address)
        :control (r/as-element [mui/radio])
        :disableTypography true
        :label (r/as-element
                 (let [selected? (= :deceased-address (get values :address))]
                   [address-box selected? deceased-address]))}])
    (for [address-text @(rf/subscribe [::model/used-billing-addresses])]
      [mui/form-control-label
       {:key (pr-str address-text)
        :value (pr-str address-text)
        :control (r/as-element [mui/radio])
        :sx {:align-items :flex-start}
        :disableTypography true
        :label (r/as-element
                 (let [selected? (= address-text (get values :address))]
                   [address-box selected? address-text]))}])
    [mui/form-control-label
     {:value (pr-str :new-address)
      :control (r/as-element [mui/radio])
      :sx {:align-items :flex-start}
      :disableTypography true
      :label (r/as-element
               (let [selected? (= :new-address (get values :address))
                     select! #(set-handle-change
                                {:value :new-address
                                 :path [:address]})]
                 [form-util/text-field fork-args
                  (merge
                    {:name :address-new
                     :hiddenLabel true
                     :placeholder "enter another address"
                     :multiline true
                     :minRows 3
                     :maxRows 3
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
                      {:inputProps {:tabindex :-1}}))]))}]]])

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

(defn form [{:keys [values] :as fork-args}]
  [mui/stack {:spacing 2}
   [type-of-bill-choice fork-args]
   (let [bill-type (get values :bill-type)]
     (cond
       (contains? bill-type :council-tax)
       [council-select fork-args]

       (seq bill-type)
       [company-select fork-args]

       :else
       [disabled-select-placeholder]))
   [account-number-field fork-args]
   [address-field fork-args]
   [amount-field fork-args]
   [meter-readings-field fork-args]])

(defn values-to-submit [{:keys [values] :as _fork-params}]
  (cond-> values
    (= :new-address (:address values)) (assoc :address (trim (:address-new values)))
    :always (dissoc :address-new)))

(def validation
  (v/join
    (v/attr [:address] (v+/v-some?))
    (fn [data]
      (when (= :new-address (:address data))
        (v/validate
          (v/attr [:address-new] (v/present))
          data)))))

(defn validate [form-data]
  (v/field-errors validation form-data))

(def initial-values
  {:address :deceased-address})
