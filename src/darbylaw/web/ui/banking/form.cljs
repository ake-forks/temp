(ns darbylaw.web.ui.banking.form
  (:require
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [fork.re-frame :as fork]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.banking.validation :as validation])
  (:require-macros [reagent-mui.util :refer [react-component]]))


(defn buildsoc-label [buildsoc-id-str]
  (assert (string? buildsoc-id-str))
  (model/asset-label :buildsoc (keyword buildsoc-id-str)))


(defn bank-select [{:keys [values set-handle-change handle-blur touched errors]}]
  (let [used-bank-ids @(rf/subscribe [::model/used-bank-ids])]
    [mui/autocomplete
     {:options (model/all-institution-ids :bank)
      :value (get values :bank-id)
      :getOptionLabel (fn [bank-id]
                        (str (model/asset-label :bank (keyword bank-id))
                          (when (contains? used-bank-ids (keyword bank-id))
                            " (already added)")))
      :getOptionDisabled (fn [bank-id] (contains? used-bank-ids (keyword bank-id)))
      :onChange (fn [_evt new-value]
                  (set-handle-change {:value new-value
                                      :path [:bank-id]}))
      :renderInput (react-component [props]
                     [mui/text-field (merge props
                                       {:name :bank-id
                                        :label "bank name"
                                        :required true
                                        :onBlur handle-blur})])}]))


(defn buildsoc-select [{:keys [values set-handle-change handle-blur touched errors]}]
  (let [used-buildsoc-ids @(rf/subscribe [::model/used-buildsoc-ids])]
    [mui/autocomplete
     {:options (model/all-institution-ids :buildsoc)
      :value (get values :buildsoc-id)
      :getOptionLabel (fn [buildsoc-id]
                        (str (model/asset-label :buildsoc (keyword buildsoc-id))
                          (when (contains? used-buildsoc-ids (keyword buildsoc-id))
                            " (already added)")))
      :getOptionDisabled (fn [buildsoc-id] (contains? used-buildsoc-ids (keyword buildsoc-id)))
      :onChange (fn [_evt new-value]
                  (set-handle-change {:value new-value
                                      :path [:buildsoc-id]}))
      :renderInput (react-component [props]
                     [mui/text-field (merge props
                                       {:name :buildsoc-id
                                        :label "building society name"
                                        :required true
                                        :onBlur handle-blur})])}]))


(defn bank-account-array
  [{:keys [stage errors values] :as props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur
                      touched]}]
  [mui/stack {:spacing 2}
   (doall
     (->> fields
       (map-indexed
         (fn [idx field]
           ^{:key idx}
           [mui/stack {:spacing 1}
            [mui/stack {:spacing 1 :direction :row}
             [mui/text-field {:name :sort-code
                              :value (or (get field :sort-code) "")
                              :label "sort code"
                              :placeholder "00-00-00"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true
                              :error (boolean (validation/get-account-error errors touched :sort-code idx))
                              :helper-text (if
                                             (boolean (validation/get-account-error errors touched :sort-code idx))
                                             "format 00-00-00")}]

             [mui/text-field {:name :account-number
                              :value (or (get field :account-number) "")
                              :label "account number"
                              :placeholder "00000000"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true
                              :error (boolean (validation/get-account-error errors touched :account-number idx))
                              :helper-text (if
                                             (boolean (validation/get-account-error errors touched :account-number idx))
                                             "8 digits")}]

             [mui/text-field {:name :estimated-value
                              :value (or (get field :estimated-value) "")
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :full-width true
                              :error (boolean (validation/get-account-error errors touched :estimated-value idx))
                              :helper-text (if
                                             (boolean (validation/get-account-error errors touched :estimated-value idx))
                                             "check formatting")
                              :disabled (= stage :valuation)
                              :InputProps
                              {:start-adornment
                               (r/as-element [mui/input-adornment {:position :start} "£"])}}]
             [mui/icon-button {:on-click #(remove idx)}
              [ui/icon-delete]]]
            (if (= stage :valuation)
              [mui/text-field {:name :confirmed-value
                               :value (get field :confirmed-value)
                               :label "confirmed value"
                               :on-change #(handle-change % idx)
                               :on-blur #(handle-blur % idx)
                               :required true
                               :full-width true
                               :error (boolean (validation/get-account-error errors touched :confirmed-value idx))
                               :helper-text (if
                                              (boolean (validation/get-account-error errors touched :confirmed-value idx))
                                              "check formatting")
                               :InputProps
                               {:start-adornment
                                (r/as-element [mui/input-adornment
                                               {:position :start} "£"])}}]
              [:<>])]))))
   [mui/button {:on-click #(insert {:sort-code "" :account-number "" :estimated-value ""})
                :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                :variant :text
                :size "large"
                :full-width false
                :start-icon (r/as-element [ui/icon-add-circle])}
    (str "add another "
      (if-let [bank-name (model/asset-label :bank (keyword (:bank-id values)))]
        (str bank-name " account")
        "account"))]])


;accounts can
(defn buildsoc-account-array
  [{:keys [stage errors values] :as props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur
                      touched]}]
  [mui/stack {:spacing 2}
   (doall
     (->> fields
       (map-indexed
         (fn [idx field]
           ^{:key idx}
           [mui/stack {:spacing 1}
            [mui/stack {:spacing 1 :direction :row}
             [mui/text-field {:name :roll-number
                              :value (get field :roll-number)
                              :label "roll number"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true
                              :error (boolean (validation/get-account-error errors touched :roll-number idx))
                              :helper-text (if
                                             (boolean (validation/get-account-error errors touched :roll-number idx))
                                             "required")}]
             [mui/text-field {:name :estimated-value
                              :value (get field :estimated-value)
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required false
                              :full-width true
                              :disabled (= stage :valuation)
                              :error (boolean (validation/get-account-error errors touched :estimated-value idx))
                              :helper-text (if
                                             (boolean (validation/get-account-error errors touched :estimated-value idx))
                                             "check formatting")
                              :InputProps
                              {:start-adornment
                               (r/as-element [mui/input-adornment
                                              {:position :start} "£"])}}]
             [mui/icon-button {:on-click #(remove idx)}
              [ui/icon-delete]]]
            (if (= stage :valuation)
              [mui/text-field {:name :confirmed-value
                               :value (get field :confirmed-value)
                               :label "confirmed value"
                               :on-change #(handle-change % idx)
                               :on-blur #(handle-blur % idx)
                               :required true
                               :full-width true
                               :error (boolean (validation/get-account-error errors touched :confirmed-value idx))
                               :helper-text (if
                                              (boolean (validation/get-account-error errors touched :confirmed-value idx))
                                              "check formatting")
                               :InputProps
                               {:start-adornment
                                (r/as-element [mui/input-adornment
                                               {:position :start} "£"])}}]
              [:<>])]))))
   [mui/button {:on-click #(insert {:roll-number "" :estimated-value ""})
                :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                :variant :text
                :size "large"
                :full-width false
                :start-icon (r/as-element [ui/icon-add-circle])}
    (str "add another "
      (if-let [buildsoc-name (model/asset-label :buildsoc (keyword (:buildsoc-id values)))]
        (str buildsoc-name " account")
        "account"))]])

(defn account-array-component [type fork-args]
  [fork/field-array {:props fork-args
                     :name :accounts}
   (case type
     :bank bank-account-array
     :buildsoc buildsoc-account-array)])

(defn accounts-unknown [{:keys [values handle-change]}]
  [mui/form-group
   [mui/form-control-label {
                            :control (r/as-element
                                       [mui/checkbox {:name :accounts-unknown
                                                      :value (:accounts-unknown values)
                                                      :checked (:accounts-unknown values)
                                                      :label "accounts not known"
                                                      :onChange handle-change}])
                            :label "account details not known"}]])

(defn submit-buttons [{:keys [left-label right-label right-disabled]}]
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between
              :sx {:width 1}}
   [mui/button {:onClick #(rf/dispatch [::model/hide-dialog])
                :variant :contained :full-width true} left-label]
   [mui/button {:type :submit
                :variant :contained
                :full-width true
                :disabled right-disabled} right-label]])

(defonce form-state (r/atom nil))

(defn form [form-component initial-values submit-fn validation-fn]
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit submit-fn
        :keywordize-keys true
        :prevent-default? true
        :initial-values initial-values
        :validation (fn [data]
                      (try
                        (validation-fn data)
                        (catch :default e
                          (js/console.error "Error during validation: " e)
                          [{:type ::validation-error :error e}])))}
       (fn [fork-args]
         [form-component (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))
