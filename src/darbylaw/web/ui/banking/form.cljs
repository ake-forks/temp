(ns darbylaw.web.ui.banking.form
  (:require
    [darbylaw.web.util.bank :as bank-utils]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [fork.re-frame :as fork]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.banking.model :as model])
  (:require-macros [reagent-mui.util :refer [react-component]]))


(defn buildsoc-label [buildsoc-id-str]
  (assert (string? buildsoc-id-str))
  (model/asset-label :buildsoc (keyword buildsoc-id-str)))

(defn bank-label [bank-id-str]
  (assert (string? bank-id-str))
  (model/asset-label :bank (keyword bank-id-str)))


(defn buildsoc-select [{:keys [values set-handle-change handle-blur touched errors] :as fork-args}]
  [mui/autocomplete
   {:options (model/all-institution-ids :buildsoc)
    :value (get values :buildsoc-id)
    :getOptionLabel buildsoc-label
    :onChange (fn [_evt new-value]
                (set-handle-change {:value new-value
                                    :path [:buildsoc-id]}))
    :renderInput (react-component [props]
                   [mui/text-field (merge props
                                     {:name :buildsoc-id
                                      :label "building society name"
                                      :required true
                                      :onBlur handle-blur})])}])

(defn bank-select [{:keys [values set-handle-change handle-blur touched errors] :as fork-args}]
  [mui/autocomplete
   {:options (model/all-institution-ids :bank)
    :value (get values :bank-id)
    :getOptionLabel bank-label
    :onChange (fn [_evt new-value]
                (set-handle-change {:value new-value
                                    :path [:bank-id]}))
    :renderInput (react-component [props]
                   [mui/text-field (merge props
                                     {:name :bank-id
                                      :label "bank name"
                                      :required true
                                      :onBlur handle-blur})])}])
(defn bank-account-array
  [{:keys [stage errors] :as props}
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
                              :error (boolean (bank-utils/get-account-error errors touched :sort-code idx))
                              :helper-text (if
                                             (boolean (bank-utils/get-account-error errors touched :sort-code idx))
                                             "format 00-00-00")}]

             [mui/text-field {:name :account-number
                              :value (or (get field :account-number) "")
                              :label "account number"
                              :placeholder "00000000"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true
                              :error (boolean (bank-utils/get-account-error errors touched :account-number idx))
                              :helper-text (if
                                             (boolean (bank-utils/get-account-error errors touched :account-number idx))
                                             "8 digits")}]

             [mui/text-field {:name :estimated-value
                              :value (or (get field :estimated-value) "")
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :full-width true
                              :error (boolean (bank-utils/get-account-error errors touched :estimated-value idx))
                              :helper-text (if
                                             (boolean (bank-utils/get-account-error errors touched :estimated-value idx))
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
      (if-let [bank-name (model/asset-label :bank (get-in props [:values :bank-id]))]
        (str bank-name " account")
        "account"))]])

;accounts can
(defn buildsoc-account-array
  [{:keys [stage] :as props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur]}]
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
                              :full-width true}]
             [mui/text-field {:name :estimated-value
                              :value (get field :estimated-value)
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required false
                              :full-width true
                              :disabled (= stage :valuation)
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
      (if-let [buildsoc-name (model/asset-label :buildsoc (get-in props [:values :buildsoc-id]))]
        (str buildsoc-name " account")
        "account"))]])

(defn account-array-component [type fork-args]
  [fork/field-array {:props fork-args
                     :name :accounts}
   (case type
     :bank bank-account-array
     :buildsoc buildsoc-account-array)])

(defn accounts-unknown [{:keys [values handle-change handle-blur touched errors] :as fork-args}]
  [mui/form-group
   [mui/form-control-label {
                            :control (r/as-element
                                       [mui/checkbox {:name :accounts-unknown
                                                      :value (:accounts-unknown values)
                                                      :checked (:accounts-unknown values)
                                                      :label "accounts not known"
                                                      :onChange handle-change}])
                            :label "account details not known"}]])

(defn submit-buttons [{:keys [left-label right-label]}]
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between
              :sx {:width 1}}
   [mui/button {:onClick #(rf/dispatch [::model/hide-dialog])
                :variant :contained :full-width true} left-label]
   [mui/button {:type :submit :variant :contained :full-width true} right-label]])


(defn form-component [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/stack {:spacing 1}
    [buildsoc-select fork-args]
    [mui/typography {:variant :body1}]]])




(defonce form-state (r/atom nil))

(defn form [form-component initial-values submit-fn]
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit submit-fn
        :keywordize-keys true
        :prevent-default? true
        :initial-values initial-values}
       (fn [fork-args]
         [form-component (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))
