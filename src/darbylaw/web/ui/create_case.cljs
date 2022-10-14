(ns darbylaw.web.ui.create-case
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require [darbylaw.web.routes :as routes]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [reagent-mui.x.date-picker :as mui-date]
            [darbylaw.web.ui :as ui]
            ["material-ui-phone-number$default" :as MuiPhoneNumber]
            [reagent-mui.material.text-field :as mui-text-field]
            [darbylaw.web.util.phone :as phone]
            [darbylaw.web.util.email :as email]
            [clojure.string :as str]))

(rf/reg-event-fx ::create-case-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (assert (:id response))
    {:db (fork/set-submitting db path false)
     ::ui/navigate-no-history [:deceased-details {:case-id (:id response)}]}))

(rf/reg-event-fx ::create-case-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}))

(defn transform-on-submit [data]
  (-> data
    (update-vals #(cond-> %
                    (string? %) clojure.string/trim))
    (update :dob #(.format % "YYYY-MM-DD"))
    (update :phone phone/format-for-storing)))

(rf/reg-event-fx ::create-case
  (fn [{:keys [db]} [_ {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri "/api/case"
        :params {:personal-representative (transform-on-submit values)}
        :on-success [::create-case-success fork-params]
        :on-failure [::create-case-failure fork-params]})}))

(defn get-error [k {:keys [touched errors attempted-submissions] :as _fork-args}]
  (and (pos? attempted-submissions)
       (touched k)
       (get errors [k])))

(defn error-icon-prop []
  {:endAdornment
   (r/as-element
     [mui/input-adornment {:position :end}
      [ui/icon-error-outline {:color :error}]])})

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

(defn title-field [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [mui/autocomplete
   {:options ["Mr" "Mrs" "Ms" "Mx" "Dr"]
    :inputValue (or (get values :title) "")
    :onInputChange (fn [_evt new-value]
                     (set-handle-change {:value new-value
                                         :path [:title]}))
    :renderInput (react-component [props]
                   [mui/text-field (merge props
                                     {:name :title
                                      :label "Title"
                                      :required true
                                      :error (boolean (get-error :title fork-args))
                                      :onBlur handle-blur})])
    :freeSolo true
    :disableClearable true
    ; no filter:
    :filterOptions identity}])

(defn name-fields [fork-args]
  [:<>
   [mui/text-field (merge (common-text-field-props :forename fork-args)
                     {:required true
                      :label "Forename"
                      :placeholder "Your legal name"
                      :full-width true})]
   [mui/text-field (merge (common-text-field-props :middlename fork-args)
                     {:label "Middle Name(s)"
                      :full-width true})]
   [mui/text-field (merge (common-text-field-props :surname fork-args)
                     {:required true
                      :label "Surname"
                      :full-width true})]])

(defn dob-text-field [fork-args]
  [mui/text-field (merge (common-text-field-props :dob fork-args)
                    {:label "Date of Birth"
                     :required true
                     :helper-text "Please use format DD/MM/YYYY"
                     :full-width true})])

(defn dob-date-picker [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [mui-date/date-picker
   {:value (get values :dob)
    :label "Date of Birth"
    :onChange #(set-handle-change {:value %
                                   :path [:dob]})
    :renderInput (fn [params]
                   (r/as-element
                     [mui/text-field
                      (merge (js->clj params)
                        {:name :dob
                         :required true
                         :error (boolean (get-error :dob fork-args))
                         :onBlur handle-blur})]))
    ; In case we want to add a helperText with the expected date pattern,
    ; (not working so far, will need some tweaking):
    ; {:helperText (-> params-clj :inputProps :placeholder)}
    :openTo :year
    :views [:year :month :day]}])

(defn address-fields [fork-args]
  [:<>
   [mui/stack {:direction :row
               :spacing 2}
    [mui/text-field (merge (common-text-field-props :flat fork-args)
                      {:label "Flat"})]
    [mui/text-field (merge (common-text-field-props :building fork-args)
                      {:label "Building Name"})]]
   [mui/stack {:direction :row
               :spacing 2}
    [mui/text-field (merge (common-text-field-props :street-number fork-args)
                      {:label "Street Number"
                       :helperText (get-error :street-number fork-args)})]
    [mui/text-field (merge (common-text-field-props :street1 fork-args)
                      {:label "Street"
                       :required true
                       :full-width true})]]
   [mui/text-field (merge (common-text-field-props :street2 fork-args)
                     {:label "Address Line 2"
                      :full-width true})]
   [mui/stack {:direction :row :spacing 2}
    [mui/text-field (merge (common-text-field-props :town fork-args)
                      {:label "Town/City"
                       :required true
                       :full-width true})]
    [mui/text-field (merge (common-text-field-props :postcode fork-args)
                      {:label "Postcode"
                       :required true})]]])

(defn phone-field [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [:> MuiPhoneNumber
   {:name :phone
    :value (get values :phone)
    :label "Mobile Phone"
    :required true
    :onChange #(set-handle-change {:value %
                                   :path [:phone]})
    :onBlur handle-blur
    :InputProps (let [error (get-error :phone fork-args)]
                  (merge
                    {:inputComponent mui-text-field/input
                     :error (boolean error)}
                    (when error
                      (error-icon-prop))))
    :defaultCountry "gb"
    :full-width true
    :variant :filled}])

(defn email-field [fork-args]
  [mui/text-field (merge (common-text-field-props :email fork-args)
                    {:label "Email"
                     :required true
                     :full-width true})])

(defn submit-button [_fork-args]
  (let [open? (r/atom false)]
    (fn [{:keys [handle-submit errors submitting?]}]
      [:<>
       ; We could be using a button of type "submit", instead of handling onclick,
       ; but then the browser will be reacting to required fields when submitting.
       ; (i.e. Chrome shows a popup on the first required field that is empty).
       [ui/loading-button {:onClick (fn [& args]
                                      (when errors
                                        (reset! open? true))
                                      (apply handle-submit args))
                           :variant :contained
                           :loading submitting?
                           :size :large}
        "Next"]
       [mui/snackbar {:open @open?
                      :autoHideDuration 6000
                      :onClose #(reset! open? false)}
        [mui/alert {:severity :error}
         "There is some missing or invalid data."]]])))

(defn personal-info-form [fork-args]
  [:form
   [mui/container {:max-width :sm
                   :sx {:mb 4}}
    [mui/stack {:spacing 4}
     [mui/stack
      [mui/typography {:variant :h3
                       :sx {:pt 4 :pb 2}}
       "get started"]
      [mui/typography {:variant :p}
       "It looks like you need probate.
      Here are some quick questions about you.
      Then we will ask about the deceased and their relationship to you."]]
     [mui/stack {:spacing 2}
      [mui/typography {:variant :h5}
       "your details"]
      [title-field fork-args]
      [mui/stack {:direction :row
                  :spacing 2}
       [name-fields fork-args]]
      [dob-date-picker fork-args]]
     [mui/stack {:spacing 2}
      [mui/typography {:variant :h5}
       "your contact details"]
      [email-field fork-args]
      [phone-field fork-args]]
     [mui/stack {:spacing 2}
      [mui/typography {:variant :h5}
       "your address"]
      [address-fields fork-args]]
     [submit-button fork-args]]]])

(defonce form-state (r/atom nil))

(defn not-nil
  ([]
   (not-nil {}))
  ([error-data]
   (v/predicate nil? (merge {:type ::not-nil} error-data))))

(defmethod v/english-translation ::not-nil
  [{:keys [name]}]
  (str name " is required."))

(defn valid-dayjs-date
  ([]
   (valid-dayjs-date {}))
  ([error-data]
   (v/predicate
     #(not (.isValid %))
     (merge {:type ::valid-dayjs-date} error-data))))

(defmethod v/english-translation ::valid-dayjs-date
  [{:keys [name]}]
  (str name " in not a valid date."))

(defn valid-phone
  ([]
   (valid-phone {}))
  ([error-data]
   (v/predicate
     #(not (phone/valid-phone? %))
     (merge {:type ::valid-phone} error-data))))

(defmethod v/english-translation ::valid-phone
  [{:keys [name]}]
  (str name " is not a valid phone."))

(defn valid-email
  ([]
   (valid-email {}))
  ([error-data]
   (v/predicate
     #(not (email/valid-email? %))
     (merge {:type ::valid-email} error-data))))

(defmethod v/english-translation ::valid-email
  [{:keys [name]}]
  (str name " is not a valid email."))

(defn present-or-alternative
  ([selector alt-selector]
   (present-or-alternative selector alt-selector {}))
  ([selector alt-selector error-data]
   (fn [data]
     (when (and (str/blank? (get-in data selector))
                (str/blank? (get-in data alt-selector)))
       [(merge
          {:type ::present-or-alternative
           :selector selector
           :alt-selector alt-selector}
          error-data)]))))

(defmethod v/english-translation ::present-or-alternative
  [{:keys [alt-selector]}]
  (str "...or provide " (do
                          (assert (= 1 (count alt-selector)))
                          (case (first alt-selector)
                            :building "Building Name"))))

(defn either-present
  ([selectors]
   (either-present selectors {}))
  ([selectors error-data]
   (fn [data]
     (when (every? str/blank? (map #(get-in data %) selectors))
       (->> selectors
         (mapv #(merge
                  {:type ::either-present
                   :selector %}
                  error-data)))))))

(defmethod v/english-translation ::either-present
  [{:keys [name]}]
  (str name " is required."))

(defn panel []
  [fork/form
   {:state form-state
    :on-submit #(rf/dispatch [::create-case %])
    :keywordize-keys true
    :prevent-default? true
    :validation
    (fn [data]
      (v/field-errors
        (v/join
          (v/attr [:title] (v/present))
          (v/attr [:forename] (v/present))
          (v/attr [:surname] (v/present))
          (v/attr [:dob] (v/chain
                           (not-nil)
                           (valid-dayjs-date)))

          ; We show Street Number as required, but provide a hint
          ; that Building Name can be provided as an alternative.
          (present-or-alternative [:street-number] [:building])
          ; alternative: show both fields as required when either is blank
          #_(either-present [[:street-number]]
                            [:building])

          (v/attr [:street1] (v/present))
          (v/attr [:town] (v/present))
          (v/attr [:postcode] (v/present))

          (v/attr [:phone] (v/chain
                             (v/present)
                             (valid-phone)))
          (v/attr [:email] (v/chain
                             (v/present)
                             (valid-email))))
        data))}
   (fn [fork-args]
     [personal-info-form (ui/mui-fork-args fork-args)])])

(defmethod routes/panels :create-case-panel [] [panel])

(comment
  ; To fill out the form programmatically:
  (do
    (def test-data
      {:street1 "Sesame", :email "test@test.com", :forename "John",
       :building "12", :phone "888999888", :town "Bristol",
       :surname "Doe", :postcode "SW1W 0NY", :title "Mr", :dob "01/01/1979"})
    (swap! form-state assoc :values test-data))
  (darbylaw.web.core/mount-root))

(comment
  ; playing with form-state
  @form-state
  (-> @form-state :values)
  (swap! form-state assoc-in [:values :phone] "+442441231235")
  (-> @form-state :values :dob)
  (-> @form-state :values :dob .isValid)
  (-> @form-state :values :dob (.format "YYYY-MM-DD")))
