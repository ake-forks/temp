(ns darbylaw.web.util.form
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require
    [fork.re-frame :as fork]
    [reagent-mui.components :as mui]
    [reagent-mui.x.date-picker :as mui-date]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [clojure.string :as str]
    [medley.core :as medley]))

(def starts-with? (fnil str/starts-with? ""))

(defn toggle-negative [s]
  (if (starts-with? s "-")
    (subs s 1)
    (str "-" s)))

(defn ensure-negative [s]
  (if (starts-with? s "-")
    s
    (str "-" s)))

(defn get-error [k {:keys [touched errors attempted-submissions] :as _fork-args}]
  (and (pos? attempted-submissions)
       (touched k)
       (get errors [k])))

(defn error-icon-prop []
  {:endAdornment
   (r/as-element
     [mui/input-adornment {:position :end}
      [#_ui/icon-error-outline
       ui/icon-edit
       #_ui/icon-priority-high
       {:color :error
        :sx {:opacity 0.4}}]])})

(defn common-input-field-props
  [k
   {:keys [values handle-change handle-blur submitting?] :as fork-args}
   {:keys [error-icon?] :as _options}]
  (let [error (get-error k fork-args)]
    (cond-> {:name k
             :value (or (get values k) "")
             :onChange handle-change
             :onBlur handle-blur
             :disabled submitting?
             :error (boolean error)
             :autoComplete :off}
      error-icon? (assoc :InputProps
                    (when error
                      (error-icon-prop))))))

(defn common-text-field-props [k fork-args]
  (common-input-field-props k fork-args {:error-icon? true}))

(defn ->FormData
  [values]
  (let [form-data (js/FormData.)]
    (doseq [[k v] values]
      (.append form-data (name k) v))
    form-data))



;; >> Wrapped MUI Fields

(defn autocomplete-field
  "A wrapped version of mui/autocomplete with support for fork forms.
  To set more just add them to the config, see: https://mui.com/material-ui/react-autocomplete/

  Config Options:
  :name          The name of the inner text field and input wrt fork
  :label         The label of the inner text field
  :options       The list of options fore the autocomplete
  :inner-config  A map of options to pass to the inner text field
  <other>        Merged on top of the built-in config

  Example:
  [autocomplete-field
   fork-args
   {:name :test
    :label \"My test autocomplete\"
    :options [:aa :ab :ba :bc :ca :cd]
    :inner-config {:required true}
    :groupBy (cljs-js first)}]"
  [{:keys [values set-handle-change handle-blur submitting?] :as fork-args}
   {:keys [name label options inner-config] :as config}]
  (assert name "Missing required arg :name")
  [mui/autocomplete
   (merge
     (let [v (get values name)]
       (if (:freeSolo config)
         {:inputValue (or v "")
          :onInputChange (fn [_evt new-value]
                           (set-handle-change {:value new-value
                                               :path [name]}))}
         {:value v
          :onChange (fn [_evt new-value]
                      (set-handle-change {:value new-value
                                          :path [name]}))}))
     {:options options
      :renderInput (react-component [props]
                     [mui/text-field
                      (merge props
                        {:name name
                         :label label
                         :error (boolean (get-error name fork-args))
                         :onBlur handle-blur}
                        inner-config)])
      :disabled submitting?}
     (dissoc config :name :options :inner-config))])

(defn text-field
  "A wrapper for mui/text-field with support for fork forms and errors
  
  Config Options:
  :name    The name of the text field and input wrt fork
  <other>  Merged on top of the built-in config"
  [{:keys [values errors handle-change handle-blur submitting?] :as fork-args}
   {:keys [name] :as config}]
  (assert name "Missing required arg :name")
  (let [error (get-error name fork-args)
        props {:name name
               :value (or (get values name) "")
               :onChange handle-change
               :onBlur handle-blur
               :error (boolean error)
               :helper-text (when error
                              (get errors [name]))
               :autoComplete :off
               :InputProps (when error (error-icon-prop))
               :disabled submitting?}
        prop-overrides (dissoc config :name)]
    [mui/text-field (merge props prop-overrides)]))

(defn submit-button
  "A wrapper for a submit button that shows an error if the form data is invalid.
  
  Config Options:
  :button        Config options merged with the button config
  :button :text  Text shown on the button
  :snackbar      Config options merged with the button config
  :alert         Config options merged with the button config
  :alert :text   Text shown in the alert
  
  Example:
  [submit-button fork-args
   {:button {:text \"Check Form\"}
    :alert {:text \"Please fix the errors\"}}]"
  [_fork-args _config]
  (let [open? (r/atom false)]
    (fn [{:keys [handle-submit errors submitting?]}
         {:keys [button snackbar alert]}]
      (let [button-text (or (:text button) "Submit")
            button (dissoc button :text)
            alert-text (or (:text alert)
                           "There is some missing or invalid data")
            alert (dissoc alert :text)]
        [:<>
         [ui/loading-button (merge
                              {:onClick (fn [& args]
                                          (when errors
                                            (reset! open? true))
                                          (apply handle-submit args))
                               :loading submitting?}
                              button)
          button-text]
         [mui/snackbar (merge
                         {:open @open?
                          :onClose #(reset! open? false)
                          :autoHideDuration 6000}
                         snackbar)
          [mui/alert (merge {:severity :error} alert)
           alert-text]]]))))

(defn date-picker
  "A wrapper to the MUI date picker with support for fork forms.
  It doesn't auto-complete manual input.

  All config can be overridden using the `config` and `inner-config` options.

  Options:
  :name          The name of the inner text field and input wrt fork
  :inner-config  A map of options to pass to the inner text field
  :inner-config :label-prefix   Set the prefix of the label, the suffix
                                is the date-pattern in brackets
  <other>        Merged on top of the built-in config

  Example:
  [date-picker fork-args
   {:name :test
    :inner-config {:label-prefix \"Test Date\"}}]"
  [{:keys [values set-handle-change handle-blur submitting?] :as fork-args}
   {:keys [name inner-config] :as config}]
  (let [{:keys [label-prefix]} inner-config
        inner-config (dissoc inner-config :label-prefix)

        date-picker-props
        {:value (get values name)
         :onChange #(set-handle-change {:value % :path [name]})
         :disabled submitting?
         :renderInput
         (react-component [props]
           [mui/text-field
            (merge props
              {:name name
               :label (let [date-pattern
                            (get-in props [:input-props :placeholder])]
                        (str label-prefix " (" date-pattern ")"))
               :autoComplete :off
               :error (-> name (get-error fork-args) boolean)
               :onBlur handle-blur}
              inner-config)])}

        prop-overrides (dissoc config :name :inner-config)]
    [mui-date/date-picker
     (merge date-picker-props prop-overrides)]))

(defn form
  "Like `fork.re-frame/form`, but also:
  - Sets some default options that we typically use
  - Wraps validation to react to errors in the validation logic
  - Adapts MUI events to be compatible with fork
  - Honors :clean-on-unmount? when passing a :state ratom"
  [props component]
  (r/with-let [{form-state :state
                :keys [clean-on-unmount?]
                :as props} (-> (merge
                                 {:clean-on-unmount? true
                                  :keywordize-keys true
                                  :prevent-default? true}
                                 props)
                             (medley/update-existing :validation
                               (fn [validation]
                                 (fn [data]
                                   (try
                                     (validation data)
                                     (catch :default e
                                       (js/console.error "Error during validation: " e)
                                       [{:type ::validation-error :error e}]))))))]
    [fork/form props
     (fn [fork-args]
       [component (ui/mui-fork-args fork-args)])]
    (finally
      (when (and (some? form-state) clean-on-unmount?)
        (reset! form-state nil
          ;; Adapted from:
          ;; https://github.com/luciodale/fork/blob/dd4da7ffbb5706cd3edbcbd4b545986ca84ea6df/src/fork/re_frame.cljs#L87
          ;; Otherwise the form breaks after sending the new code
          #_(merge (when (:keywordize-keys props)
                     {:keywordize-keys true})
              {:values {} :touched #{}}))))))
