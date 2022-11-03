(ns darbylaw.web.util.form
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require [reagent-mui.components :as mui]
            [reagent-mui.x.date-picker :as mui-date]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]))

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
   {:keys [values handle-change handle-blur] :as fork-args}
   {:keys [error-icon?] :as _options}]
  (let [error (get-error k fork-args)]
    (cond-> {:name k
             :value (get values k)
             :onChange handle-change
             :onBlur handle-blur
             :error (boolean error)
             :autoComplete :off}
      error-icon? (assoc :InputProps
                    (when error
                      (error-icon-prop))))))

(defn common-text-field-props [k fork-args]
  (common-input-field-props k fork-args {:error-icon? true}))



;; >> Wrapped MUI Fields

(defn autocomplete-field
  "A wrapped version of mui/autocomplete with the following features:
  - Support for fork forms
  - Free text input enabled
  - Clearing desabled
  - No auto-filtering options
  To set more just add them to the config, see: https://mui.com/material-ui/react-autocomplete/

  Config Options:
  :name          The name of the inner text field and input wrt fork
  :label         The label of the inner text field
  :options       The list of options fore the autocomplete
  :inner-config  A map of options to pass to the inner text field
  <other>        Merged ontop of the built-in config

  Example:
  [autocomplete-field
   fork-args
   {:name :test
    :label \"My test autocomplete\"
    :options [:aa :ab :ba :bc :ca :cd]
    :inner-config {:required true}
    :groupBy (cljs-js first)}]"
  [{:keys [values set-handle-change handle-blur] :as fork-args}
   {:keys [name label options inner-config] :as config}]
  (assert name "Missing required arg :name")
  (let [autocomplete-props 
        {:options options
         :inputValue (or (get values name) "")
         :onInputChange (fn [_evt new-value]
                          (set-handle-change {:value new-value
                                              :path [name]}))
         :renderInput (react-component [props]
                        [mui/text-field 
                         (merge props
                                {:name name
                                 :label label
                                 :error (boolean (get-error name fork-args))
                                 :onBlur handle-blur}
                                inner-config)])
         ;; Allow free text input
         :freeSolo true
         ;; Don't allow clearing input
         :disableClearable true
         ;; Don't filter results
         :filterOptions identity}

        prop-overrides
        (dissoc config :name :options :inner-config)]
    [mui/autocomplete (merge autocomplete-props prop-overrides)]))

(defn text-field
  "A wrapper for mui/text-field with support for fork forms and errors
  
  Config Options:
  :name    The name of the text field and input wrt fork
  <other>  Merged ontop of the built-in config"
  [{:keys [values handle-change handle-blur] :as fork-args}
   {:keys [name] :as config}]
  (assert name "Missing required arg :name")
  (let [error (get-error name fork-args)
        props {:name name
               :value (get values name)
               :onChange handle-change
               :onBlur handle-blur
               :error (boolean error)
               :autoComplete :off
               :InputProps (when error (error-icon-prop))}
        prop-overrides (dissoc config :name)]
    [mui/text-field (merge props prop-overrides)]))

(defn submit-button [_fork-args _config]
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
  Has the followoing set by default:
  - Open to year
  - Show year, month then day
  - Don't auto-complete manual input

  All config can be overridden using the `config` and `inner-config` options.

  Options:
  :name          The name of the inner text field and input wrt fork
  :inner-config  A map of options to pass to the inner text field
  :inner-config :label-prefix   Set the prefix of the label, the suffix
                                is the date-pattern in brackets
  <other>        Merged ontop of the built-in config

  Example:
  [date-picker fork-args
   {:name :test
    :inner-config {:label-prefix \"Test Date\"}}]"
  [{:keys [values set-handle-change handle-blur] :as fork-args}
   {:keys [name inner-config] :as config}]
  (let [{:keys [label-prefix]} inner-config
        inner-config (dissoc inner-config :label-prefix)

        date-picker-props
        {:value (get values name)
         :onChange #(set-handle-change {:value % :path [name]})
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
              inner-config)])
         :openTo :year
         :views [:year :month :day]}

        prop-overrides (dissoc config :name :inner-config)]
    [mui-date/date-picker
     (merge date-picker-props prop-overrides)]))
