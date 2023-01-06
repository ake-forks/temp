(ns darbylaw.web.ui.funeral.account.form
  (:require
    [reagent-mui.components :as mui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.util :as util]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form]
    [vlad.core :as v]
    [darbylaw.web.util.vlad :as v-util]))

(defn submit-buttons [{:keys [submitting?] :as fork-args}]
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between}
   [mui/button {:variant :contained
                :full-width true
                :on-click
                #(rf/dispatch [::funeral-model/hide-funeral-dialog])}
    "cancel"]
   [mui/button {:type :submit
                :full-width true
                :variant :contained
                :disabled submitting?}
    "save"]])

(defn title-field [fork-args]
  [form/text-field fork-args
   {:name :title
    :label "institution"
    :required true}])

(defn amount-field [fork-args]
  [form/text-field fork-args
   {:name :value
    :label "amount"
    :full-width true
    :required true
    :InputProps
    {:start-adornment
     (r/as-element
       [mui/input-adornment {:position :start} "£"])}}])

(defn paid-field [{:keys [values handle-change] :as fork-args}]
  [mui/form-control-label
   {:label "paid?"
    :control
    (r/as-element
      [mui/checkbox
       {:name :paid?
        :value (:paid? values false)
        :checked (:paid? values false)
        :on-change #(handle-change %)}])}])

(defn funeral-file-url
  [case-id file-name]
  (str "/api/case/" case-id "/funeral/account/" file-name))

(defn receipt-field [fork-args]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        account @(rf/subscribe [::funeral-model/account])]
    [mui/stack {:direction :row
                :spacing 1
                :justify-content :space-between}
     [util/upload-button fork-args
      {:name :receipt
       :full-width true}]
     [util/download-button
      {:full-width true
       :href (funeral-file-url case-id "receipt")
       :disabled (not (:receipt-uploaded account))}]]))

(defn invoice-field [fork-args]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        account @(rf/subscribe [::funeral-model/account])]
    [mui/stack {:direction :row
                :spacing 1
                :justify-content :space-between}
     [util/upload-button fork-args
      {:name :invoice
       :full-width true}]
     [util/download-button
      {:full-width true
       :href (funeral-file-url case-id "invoice")
       :disabled (not (:invoice-uploaded account))}]]))

(defn paid-by-field [fork-args]
  [form/text-field fork-args
   {:name :paid-by
    :label "paid by"
    :full-width true}])

(defn layout [{:keys [values handle-submit]
               :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/stack {:spacing 1}
    [title-field fork-args]

    [mui/stack {:direction :row
                :spacing 1}
     [amount-field fork-args]
     [paid-field fork-args]]

    (if (:paid? values)
      [:<>
       [paid-by-field fork-args]
       [mui/typography "receipt"]
       [receipt-field fork-args]]

      [:<>
       [mui/typography "invoice"]
       [invoice-field fork-args]])
      
    [submit-buttons]]])

(def data-validation
  (v/join
    (v/attr [:title] (v/present))
    (v/attr [:value] (v/chain (v/present) (v-util/currency?)))))

(defn form [values on-submit]
  (r/with-let [form-state (r/atom nil)]
    [fork/form
     {:state form-state
      :clean-on-unmount true
      :on-submit on-submit
      :keywordize-keys true
      :prevent-default? true
      :validation (fn [data]
                    (try (v/field-errors data-validation data)
                      (catch :default e
                        (js/console.log "Error during validation: " e)
                        [{:type ::validation-error :error e}])))
      :initial-values values}
     (fn [fork-args]
        [layout (ui/mui-fork-args fork-args)])]
    (finally
      (reset! form-state nil))))
