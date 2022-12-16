(ns darbylaw.web.ui.funeral.other.form
  (:require
    [reagent-mui.components :as mui]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form]
    [vlad.core :as v]))

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

(defn file-upload-field
  [fork-args
   {:keys [name label] :as config}]
  (assert name "Missing required arg :name")
  (let [props {:name name
               :label label
               :full-width true}
        prop-overrides (dissoc config :name :label :button-config)

        button-label (or (get-in config [:button-config :label])
                         "choose file")
        button-props {:variant :contained}
        button-prop-overrides (-> config :button-config (dissoc :label))]
    [mui/stack {:direction :row
                :spacing 1}
     [form/text-field fork-args
      (merge props prop-overrides)]
     [mui/button
      (merge button-props button-prop-overrides)
      button-label]]))

(defn recipt-field [fork-args]
  [file-upload-field fork-args
   {:name :recipt
    :label "recipt"
    :button-config {:on-click #(println "test")}}])

(defn invoice-field [fork-args]
  [file-upload-field fork-args
   {:name :invoice
    :label "invoice"
    :button-config {:on-click #(println "test")}}])

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

    [recipt-field fork-args]
    [paid-by-field fork-args]
      
    [submit-buttons]]])

(def currency?
  (v/matches #"[0-9]+(\.[0-9]{2})?"))

(def data-validation
  (v/join
    (v/attr [:title] (v/present))
    (v/attr [:value] (v/chain
                       (v/present)
                       currency?))))

(defn form [values on-submit]
  (r/with-let [form-state (r/atom nil)]
    [fork/form
     {:state form-state
      :clean-on-unmount true
      :on-submit on-submit
      :keywordize-keys true
      :prevent-default? true
      ;:validation (fn [data]
      ;              (try (v/field-errors data-validation data)
      ;                (catch :default e
      ;                  (js/console.log "Error during validation: " e)
      ;                  [{:type ::validation-error :error e}]))
      :initial-values values}
     (fn [fork-args]
        [layout (ui/mui-fork-args fork-args)])]
    (finally
      (reset! form-state nil))))
