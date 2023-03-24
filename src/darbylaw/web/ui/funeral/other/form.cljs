(ns darbylaw.web.ui.funeral.other.form
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
    :label "expense name"
    :required true}])

(defn amount-field [{:keys [values set-values] :as fork-args}]
  [:<>
   [form/text-field fork-args
    {:name :value
     :label "amount"
     :full-width true
     :required true
     :InputProps
     {:start-adornment
      (r/as-element
        [mui/input-adornment {:position :start} "£"])}}]
   [mui/form-control-label
    {:label "debt?"
     :checked (-> values :value (or "") (form/starts-with? "-"))
     :on-click #(set-values {:value (-> values :value form/toggle-negative)})
     :control (r/as-element [mui/switch])}]])

(defn paid-field [{:keys [values handle-change] :as fork-args}]
  [mui/form-control-label
   {:label "paid?"
    :control
    (r/as-element
      [mui/checkbox
       {:name :paid
        :value (:paid values false)
        :checked (:paid values false)
        :on-change #(handle-change %)}])}])

(defn funeral-file-url
  [case-id expense-id]
  (str "/api/case/" case-id "/funeral/other/" expense-id "/receipt"))

(defn receipt-field [{:keys [touched errors] :as fork-args}]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        expense-id @(rf/subscribe [::funeral-model/dialog-info])]
    [mui/stack {:spacing 1}
     [mui/stack {:direction :row
                 :spacing 1
                 :justify-content :space-between}
      [util/upload-button fork-args
       {:name :receipt
        :full-width true}]
      [util/download-button
       {:full-width true
        :href (funeral-file-url case-id expense-id)
        :disabled (and (not (nil? expense-id))
                       (let [expense @(rf/subscribe [::funeral-model/expense expense-id])]
                         (not (contains? expense :receipt))))}]]
     (when (and (touched :receipt)
                (get errors [:receipt]))
       [mui/alert {:severity :error}
        [mui/alert-title "validation error"]
        "receipt is required"])]))

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

    (when (:paid values)
      [:<>
       [paid-by-field fork-args]
       [mui/typography "receipt"]
       [receipt-field fork-args]])
      
    [submit-buttons fork-args]]])

(def data-validation
  (v/join
    (v/attr [:title] (v/present))
    (v/attr [:value] (v/chain
                       (v/present)
                       (v-util/currency?)
                       (v-util/string-negative?)))
    (v-util/v-when #(true? (:paid %))
      (v/attr [:receipt] (v-util/not-nil)))))

(def default-values
  {:value "-"})

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
      :initial-values (or values default-values)}
     (fn [fork-args]
        [layout (ui/mui-fork-args fork-args)])]
    (finally
      (reset! form-state nil))))
