(ns darbylaw.web.ui.deceased-details-form
  (:require [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.vlad :as v-utils]
            [reagent.core :as r]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.util.dayjs :as dayjs]))

(defonce form-state (r/atom nil))

(defn adapt-initial-values [initial-values]
  (-> initial-values
    (update :date-of-birth dayjs/maybe-read)
    (update :date-of-death dayjs/maybe-read)))

(rf/reg-fx ::reset-form!
  (fn [[{:keys [reset]} response]]
    (let [new-values (adapt-initial-values response)]
      (reset {:initial-values new-values
              :values new-values}))))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ create|edit
                     case-id
                     {:keys [path] :as fork-params}
                     response]]
    (merge
      {:db (fork/set-submitting db path false)}
      (case create|edit
        :create {::ui/navigate-no-history [:dashboard {:case-id case-id}]}
        :edit {::reset-form! [fork-params response]
               :dispatch [::case-model/load-case! case-id]}))))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ {:keys [path]} _response]]
    {:db (fork/set-submitting db path false)}))

(defn transform-on-submit [data]
  (-> data
    (update-vals #(cond-> %
                    (string? %) clojure.string/trim))
    (update :date-of-death dayjs/format-date-for-store)
    (update :date-of-birth dayjs/format-date-for-store)))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} [_ create|edit case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :put
        :uri (str "/api/case/" case-id "/deceased")
        :params (transform-on-submit values)
        :on-success [::submit-success create|edit case-id fork-params]
        :on-failure [::submit-failure fork-params]})}))

(def relationships
  ["mother"
   "father"
   "grandmother"
   "grandfather"
   "wife"
   "husband"
   "sister"
   "brother"
   "child"
   "cousin"
   "aunt"
   "uncle"
   "stepparent"
   "friend"])

(defn relationship-field [fork-args]
  [form/autocomplete-field fork-args
   {:name :relationship
    :label "Your relationship with the deceased"
    :options relationships
    :inner-config {:required true}
    :disableClearable true}])

(defn name-fields [fork-args]
  [:<>
   [form/text-field fork-args
    {:name :forename
     :label "Forename"
     :required true
     :full-width true}]
   [form/text-field fork-args
    {:name :middlename
     :label "Middle Name(s)"
     :full-width true}]
   [form/text-field fork-args
    {:name :surname
     :label "Surname"
     :required true
     :full-width true}]])

(defn deceased-details-form* [create|edit {:keys [dirty] :as fork-args}]
  [:form
   [mui/stack {:spacing 4}
    [mui/stack {:spacing 2}
     [mui/typography {:variant :p}
      "We need these details so that we can... {TODO}"]

     [mui/accordion {:sx {:backgroundColor "rgb(255, 244, 229)"}}
      [mui/accordion-summary
       [mui/typography "Notes"]]
      [mui/accordion-details
       [ui/???_TO_BE_DEFINED_??? "Add a helper to point to locations on Death Certificate? Like on Credit Card"]
       [ui/???_TO_BE_DEFINED_??? "Or at least an explanation of what some fields mean"]
       [ui/???_TO_BE_DEFINED_??? "Maybe an example of valid input?"]]]]

    [mui/stack {:spacing 2}
     [relationship-field fork-args]]
    [mui/stack {:spacing 2}
     [mui/typography {:variant :h5}
      "the deceased"]
     [form/text-field fork-args
      {:name :registration-district
       :label "Registration District"
       :required true}]
     [mui/stack {:direction :row :spacing 2}
      [form/text-field fork-args
       {:name :administrative-area
        :label "Parish (if specified) and County"
        :required true
        :sx {:width "65%"}}]
      [form/text-field fork-args
       {:name :entry-number
        :label "Entry Number"
        :required true
        :sx {:width "35%"}}]]
     [form/date-picker fork-args
      {:name :date-of-death
       :inner-config {:label-prefix "Date of Death"
                      :required true}}]
     [form/text-field fork-args
           {:name :place-of-death
            :label "Place of Death"
            :required true}]]
    [mui/stack {:spacing 2}
     [mui/stack {:direction :row :spacing 2}
      [name-fields fork-args]]
     [mui/stack {:direction :row :spacing 2}
      ;; `You can only apply to be recognised as male or female. Non-binary genders are not legally recognised in the UK.`
      ;; https://www.gov.uk/apply-gender-recognition-certificate
      [form/autocomplete-field fork-args
       {:name :sex
        :label "Legal Sex"
        :options ["female" "male"]
        :inner-config {:required true}
        :disableClearable true
        :full-width true}]
      [form/text-field fork-args
       {:name :maiden-name
        :label "Maiden Name (if applicable)"
        :full-width true}]]
     [ui/???_TO_BE_DEFINED_??? "Add a little (i) here linking to gov.uk?"]
     [form/date-picker fork-args
      {:name :date-of-birth
       :inner-config {:label-prefix "Date of Birth"
                      :required true}
       :openTo :year
       :views [:year :month :day]}]
     [form/text-field fork-args
      {:name :place-of-birth
       :label "Place of Birth"
       :required true}]
     [form/text-field fork-args
      {:name :occupation
       :label "Occupation"
       :required true}]]

    [mui/stack {:spacing 2}
     [form/text-field fork-args
      {:name :name-of-informant
       :label "Name of Informant"
       :required true}]
     [form/text-field fork-args
      {:name :cause-of-death
       :label "Cause of Death"
       :required true}]
     [form/text-field fork-args
      {:name :name-of-doctor-certifying
       :label "Name of Doctor Certifying Death"
       :required true}]
     [form/text-field fork-args
      {:name :name-of-registrar
       :label "Name of Registrar"
       :required true}]]

    [form/submit-button fork-args
     {:button {:text (case create|edit
                       :create "Create Case"
                       :edit "Save")
               :variant :contained
               :size :large
               :disabled (and (= create|edit :edit)
                              (not dirty))}}]]])

(def data-validation
  (v/join
    (v/attr [:relationship] (v/present))

    (v/attr [:registration-district] (v/present))
    (v/attr [:administrative-area] (v/present))
    (v/attr [:entry-number] (v/present))

    (v/attr [:date-of-death]
      (v/chain
        (v-utils/not-nil)
        (v-utils/valid-dayjs-date)))
    (v/attr [:place-of-death] (v/present))

    (v/attr [:forename] (v/present))
    (v/attr [:surname] (v/present))
    (v/attr [:sex] (v/present))

    (v/attr [:date-of-birth]
      (v/chain
        (v-utils/not-nil)
        (v-utils/valid-dayjs-date)))
    (v/attr [:place-of-birth]  (v/present))

    (v/attr [:occupation] (v/present))

    (v/attr [:name-of-informant] (v/present))
    (v/attr [:cause-of-death] (v/present))
    (v/attr [:name-of-doctor-certifying] (v/present))
    (v/attr [:name-of-registrar] (v/present))))

(defn deceased-details-form [create|edit {:keys [initial-values]}]
  (let [route-params @(rf/subscribe [::ui/path-params])]
    [fork/form
     {:state form-state
      :clean-on-unmount? true
      :on-submit (let [case-id (:case-id route-params)]
                   (assert case-id)
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
       [deceased-details-form* create|edit (ui/mui-fork-args fork-args)])]))

(comment
  ; To fill out the form programmatically:
  (do
    (def test-data
      {:forename "forename",
       :sex "female",
       :entry-number "entry-number",
       :name-of-informant "informant",
       :date-of-death (dayjs/read "2022-11-05"),
       :registration-district "registration district",
       :occupation "occupation",
       :relationship "mother",
       :surname "surname",
       :date-of-birth (dayjs/read "1982-01-06"),
       :middlename "middlename",
       :cause-of-death "cause of death",
       :name-of-doctor-certifying "doctor",
       :name-of-registrar "registrar",
       :maiden-name "maiden name",
       :place-of-death "place of death",
       :place-of-birth "place of birth",
       :administrative-area "parish"})
    (swap! form-state assoc :values test-data)
    (darbylaw.web.core/mount-root)))
