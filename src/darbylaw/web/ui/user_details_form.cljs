(ns darbylaw.web.ui.user-details-form
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [reagent-mui.x.date-picker :as mui-date]
            [darbylaw.web.ui :as ui]
            ["material-ui-phone-number-2$default" :as MuiPhoneNumber]
            [reagent-mui.material.text-field :as mui-text-field]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.phone :as phone]
            [darbylaw.web.util.vlad :as v-utils]
            [applied-science.js-interop :as j]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.util.dayjs :as dayjs]
            [clojure.string :as str]))

(defn adapt-initial-values [initial-values]
  (-> initial-values
    (update :date-of-birth dayjs/maybe-read)))

(rf/reg-fx ::reset-form!
  (fn [[{:keys [reset]} response]]
    (let [new-values (adapt-initial-values response)]
      (reset {:initial-values new-values
              :values new-values}))))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ create|edit {:keys [path] :as fork-params} response]]
    (merge
      {:db (fork/set-submitting db path false)}
      (case create|edit
        :create {::ui/navigate-no-history
                 [:create-deceased-details {:case-id (let [case-id (:id response)]
                                                       (assert case-id)
                                                       case-id)}]}
        :edit {::reset-form! [fork-params response]}))))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ _create|edit {:keys [path]} _response]]
    {:db (fork/set-submitting db path false)}))

(defn transform-on-submit [data]
  (-> data
    (update-vals #(cond-> %
                    (string? %) clojure.string/trim))
    (update :date-of-birth dayjs/format-date-for-store)
    (update :phone phone/format-for-storing)
    (->>
      (remove (comp str/blank? val))
      (into {}))))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} [_ create|edit case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       (merge
         (case create|edit
           :create {:method :post
                    :uri "/api/case"
                    :params {:personal-representative (transform-on-submit values)}}
           :edit {:method :put
                  :uri (str "/api/case/" case-id "/personal-representative")
                  :params (transform-on-submit values)})
         {:on-success [::submit-success create|edit fork-params]
          :on-failure [::submit-failure create|edit fork-params]}))}))

(defn title-field [fork-args]
  [form/autocomplete-field fork-args
   {:name :title
    :label "Title"
    :options ["Mr" "Mrs" "Ms" "Mx" "Dr"]
    :inner-config {:required true}
    :freeSolo true
    :disableClearable true
    ;; Don't filter results
    :filterOptions identity}])

(defn name-fields [fork-args]
  [:<>
   [mui/text-field (merge (form/common-text-field-props :forename fork-args)
                     {:required true
                      :label "Forename"
                      :placeholder "Your legal name"
                      :full-width true})]
   [mui/text-field (merge (form/common-text-field-props :middlename fork-args)
                     {:label "Middle Name(s)"
                      :full-width true})]
   [mui/text-field (merge (form/common-text-field-props :surname fork-args)
                     {:required true
                      :label "Surname"
                      :full-width true})]])

(defn date-of-birth-picker [{:keys [values set-handle-change handle-blur submitting?] :as fork-args}]
  [mui-date/date-picker
   {:value (get values :date-of-birth)
    :onChange #(set-handle-change {:value %
                                   :path [:date-of-birth]})
    :disabled submitting?
    :renderInput
    (fn [params]
      (r/as-element
        [mui/text-field
         (merge (js->clj params)
           {:name :date-of-birth
            :label (let [date-pattern (j/get-in params [:inputProps :placeholder])]
                     (str "Date of Birth (" date-pattern ")"))
            :required true
            :autoComplete :off
            :error (boolean (form/get-error :date-of-birth fork-args))
            :onBlur handle-blur})]))
    :openTo :year
    :views [:year :month :day]}])

(defn address-fields [fork-args]
  [:<>
   [mui/stack {:direction :row
               :spacing 2}
    [mui/text-field (merge (form/common-text-field-props :flat fork-args)
                      {:label "Flat"})]
    [mui/text-field (merge (form/common-text-field-props :building fork-args)
                      {:label "Building Name"})]]
   [mui/stack {:direction :row
               :spacing 2}
    [mui/text-field (merge (form/common-text-field-props :street-number fork-args)
                      {:label "Street Number"
                       :helperText (form/get-error :street-number fork-args)})]
    [mui/text-field (merge (form/common-text-field-props :street1 fork-args)
                      {:label "Street"
                       :required true
                       :full-width true})]]
   [mui/text-field (merge (form/common-text-field-props :street2 fork-args)
                     {:label "Address Line 2"
                      :full-width true})]
   [mui/stack {:direction :row :spacing 2}
    [mui/text-field (merge (form/common-text-field-props :town fork-args)
                      {:label "Town/City"
                       :required true
                       :full-width true})]
    [mui/text-field (merge (form/common-text-field-props :postcode fork-args)
                      {:label "Postcode"
                       :required true})]]])

(defn phone-field [{:keys [values set-handle-change handle-blur submitting?] :as fork-args}]
  [:> MuiPhoneNumber
   {:name :phone
    :value (get values :phone)
    :label "Mobile Phone"
    :required true
    :onChange #(set-handle-change {:value %
                                   :path [:phone]})
    :disabled submitting?
    :onBlur handle-blur
    :InputProps (let [error (form/get-error :phone fork-args)]
                  (merge
                    {:inputComponent mui-text-field/input
                     :error (boolean error)}
                    (when error
                      (form/error-icon-prop))))
    :defaultCountry "gb"
    :full-width true
    :variant :filled}])

(defn email-field [fork-args]
  [mui/text-field (merge (form/common-text-field-props :email fork-args)
                    {:label "Email"
                     :required true
                     :full-width true})])

(defn submit-button [_create|edit _fork-args]
  (let [open? (r/atom false)]
    (fn [create|edit {:keys [handle-submit errors submitting? dirty]}]
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
                           :size :large
                           :disabled (and (= create|edit :edit)
                                          (not dirty))}
        (case create|edit
          :create "Next"
          :edit "Save")]
       [mui/snackbar {:open @open?
                      :autoHideDuration 6000
                      :onClose #(reset! open? false)}
        [mui/alert {:severity :error}
         "There is some missing or invalid data."]]])))

(defn personal-info-form-fields [create|edit fork-args]
  [:<>
   [mui/stack {:spacing 2}
    (when (not= create|edit :edit)
      [mui/typography {:variant :h5}
       "your details"])
    [title-field fork-args]
    [mui/stack {:direction :row
                :spacing 2}
     [name-fields fork-args]]
    [date-of-birth-picker fork-args]]
   [mui/stack {:spacing 2}
    [mui/typography {:variant :h5}
     "your contact details"]
    [email-field fork-args]
    [phone-field fork-args]]
   [mui/stack {:spacing 2}
    [mui/typography {:variant :h5}
     "your address"]
    [address-fields fork-args]]])

(def data-validation
  (v/join
    (v/attr [:title] (v/present))
    (v/attr [:forename] (v/present))
    (v/attr [:surname] (v/present))
    (v/attr [:date-of-birth] (v/chain
                               (v-utils/not-nil)
                               (v-utils/valid-dayjs-date)))

    (v/attr [:email] (v/chain
                       (v/present)
                       (v-utils/valid-email)))
    (v/attr [:phone] (v/chain
                       (v/present)
                       (v-utils/valid-phone)))

    ; We show Street Number as required, but provide a hint
    ; that Building Name can be provided as an alternative.
    (v-utils/present-or-alternative [:street-number] [:building])
    ; alternative: show both fields as required when either is blank
    #_(v-utils/either-present [[:street-number]]
        [:building])

    (v/attr [:street1] (v/present))
    (v/attr [:town] (v/present))
    (v/attr [:postcode] (v/present))))

(defonce form-state (r/atom nil))

(defn user-details-form [create|edit {:keys [initial-values]}]
  (r/with-let []
    [fork/form
     {:state form-state
      :clean-on-unmount? true
      :on-submit (let [case-id (when (= create|edit :edit)
                                 @(rf/subscribe [::case-model/case-id]))]
                   #(rf/dispatch [::submit create|edit case-id %]))
      :keywordize-keys true
      :prevent-default? true
      :initial-values (adapt-initial-values initial-values)
      :validation (fn [data]
                    (try
                      (v/field-errors data-validation data)
                      (catch :default e
                        (js/console.error "Error during validation: " e)
                        [{:type ::validation-error :error e}])))}
     (fn [fork-args]
       (let [fork-args (ui/mui-fork-args fork-args)]
         [:form
          [mui/stack {:spacing 4}
           [personal-info-form-fields create|edit fork-args]
           [submit-button create|edit fork-args]]]))]
    (finally
      (reset! form-state nil))))

(defn dev-auto-fill []
  "Fill out the form programmatically.
  For development purposes only."
  (let [test-data {:title "Mr",
                   :forename "John",
                   :surname "Doe",
                   :date-of-birth (dayjs/read "1979-12-13")
                   :email "test@test.com",
                   :phone "+441234123456",
                   :street-number "12",
                   :street1 "Sesame",
                   :town "Bristol",
                   :postcode "SW1W 0NY"}]
   (swap! form-state assoc :values test-data)))

(comment
  (do
    (dev-auto-fill)
    (darbylaw.web.core/mount-root)))

(comment
  ; playing with form-state
  @form-state
  (-> @form-state :values)
  (swap! form-state assoc-in [:values :phone] "+442441231235")
  (-> @form-state :values :date-of-birth)
  (-> @form-state :values :date-of-birth .isValid)
  (-> @form-state :values :date-of-birth (.format "YYYY-MM-DD")))
