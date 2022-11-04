(ns darbylaw.web.ui.deceased-details-form
  (:require [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.vlad :as v-utils]
            [reagent.core :as r]))

(defonce form-state (r/atom nil))

(defn dispose []
  (reset! form-state nil))

(defn adapt-initial-values [initial-values]
  (merge
    {:relationship ""
     :sex ""}
    initial-values))

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
        :edit {::reset-form! [fork-params response]}))))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ {:keys [path]} _response]]
    {:db (fork/set-submitting db path false)}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} [_ create|edit case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :put
        :uri (str "/api/case/" case-id "/deceased")
        :params values
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
  ;; TODO: Fix so that it actually searches it's options
  [form/autocomplete-field fork-args
   {:name :relationship
    :label "Your relationship with the deceased"
    :options relationships
    ;; Disable freeSolo?
    :inner-config {:required true}}])

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

(defn deceased-details-form [create|edit {:keys [dirty] :as fork-args}]
  [:form
   [mui/container {:max-width :sm :sx {:mb 4}}
    [mui/stack {:spacing 4}
     [mui/stack
      [mui/typography {:variant :h3 :sx {:pt 4 :pb 2}}
       "deceased's details"]
      [mui/typography {:variant :p}
       "We need these details so that we can... {TODO}"]]

     [mui/accordion {:sx {:backgroundColor "rgb(255, 244, 229)"}}
      [mui/accordion-summary
       [mui/typography "Notes"]]
      [mui/accordion-details
       [ui/???_TO_BE_DEFINED_??? "Add a helper to point to locations on Death Certificate? Like on Credit Card"]
       [ui/???_TO_BE_DEFINED_??? "Or at least an explanation of what some fields mean"]
       [ui/???_TO_BE_DEFINED_??? "Maybe an example of valid input?"]]]

     [mui/stack {:spacing 2}

      [mui/typography {:variant :h5}
       "you"]

      [relationship-field fork-args]

      [mui/typography {:variant :h5}
       "the deceased"]

      [mui/stack {:direction :row :spacing 2}
       [name-fields fork-args]]

      [form/text-field fork-args
       {:name :maiden-name
        :label "Maiden Name (if applicable)"}]

      [ui/???_TO_BE_DEFINED_??? "Add a little (i) here linking to gov.uk?"]

      ;; `You can only apply to be recognised as male or female. Non-binary genders are not legally recognised in the UK.`
      ;; https://www.gov.uk/apply-gender-recognition-certificate
      ;[form/autocomplete-field fork-args
      ; {:name :sex
      ;  :label "Legal Sex"
      ;  :options ["Female" "Male"]
      ;  :freeSolo false
      ;  :inner-config {:required true}}]
      ;
      ;[mui/stack {:direction :row :spacing 1}
      ; [form/date-picker fork-args
      ;  {:name :date-of-birth
      ;   :inner-config {:label-prefix "Date of Birth"
      ;                  :required true}}]
      ;
      ; [form/date-picker fork-args
      ;  {:name :date-of-death
      ;   :inner-config {:label-prefix "Date of Death"
      ;                  :required true}}]]
      ;
      ;[form/text-field fork-args
      ; {:name :occupation
      ;  :label "Occupation"
      ;  :required true}]
      ;
      ;[form/text-field fork-args
      ; {:name :place-of-death
      ;  :label "Place of Death"
      ;  :required true}]
      ;
      ;[form/text-field fork-args
      ; {:name :cause-of-death
      ;  :label "Cause of Death"
      ;  :required true}]
      ;
      ;[mui/typography {:variant :h5}
      ; "the death certificate"]
      ;
      ;[mui/stack {:direction :row :spacing 2}
      ;
      ; [form/text-field fork-args
      ;  {:name :registration-district
      ;   :label "Registration District"
      ;   :required true
      ;   :sx {:width "65%"}}]
      ;
      ; [form/text-field fork-args
      ;  {:name :entry-number
      ;   :label "Entry Number"
      ;   :required true
      ;   :sx {:width "35%"}}]]
      ;
      ;[form/text-field fork-args
      ; {:name :administrative-area
      ;  :label "Parish (if specified) and County"
      ;  :required true}]
      ;
      ;[mui/stack {:spacing 1}
      ; [form/text-field fork-args
      ;  {:name :name-of-doctor-certifying
      ;   :label "Name of Doctor Certifying Death"
      ;   :required true}]
      ;
      ; [form/text-field fork-args
      ;  {:name :name-of-informant
      ;   :label "Name of Informant"
      ;   :required true}]
      ;
      ; [form/text-field fork-args
      ;  {:name :name-of-registrar
      ;   :label "Name of Registrar"
      ;   :required true}]]
      ;
      [form/submit-button fork-args
       {:button {:text (case create|edit
                         :create "Create Case"
                         :edit "Save")
                 :variant :contained
                 :disabled (and (= create|edit :edit)
                                (not dirty))}}]]]]])

(defn panel [create|edit {:keys [initial-values]}]
  (let [route-params @(rf/subscribe [::ui/path-params])]
    [mui/container {:max-width :md}
     [fork/form
      {:state form-state
       :on-submit (let [case-id (:case-id route-params)]
                    (assert case-id)
                    #(rf/dispatch [::submit create|edit case-id %]))
       :keywordize-keys true
       :prevent-default? true
       :initial-values (adapt-initial-values initial-values)
       :validation
       (fn [data]
         (v/field-errors
           (v/join
             (v/attr [:relationship] (v/present))
             (v/attr [:forename] (v/present))
             (v/attr [:surname] (v/present)))
           ;(v/attr [:sex]
           ;        (v/chain
           ;          (v/present)
           ;          (v/one-of #{"Male" "Female"})))
           ;(v/attr [:date-of-birth]
           ;        (v/chain
           ;          (v-utils/not-nil)
           ;          (v-utils/valid-dayjs-date)))
           ;(v/attr [:date-of-death]
           ;        (v/chain
           ;          (v-utils/not-nil)
           ;          (v-utils/valid-dayjs-date)))
           ;(v/attr [:occupation] (v/present))
           ;(v/attr [:place-of-death] (v/present))
           ;(v/attr [:cause-of-death] (v/present))
           ;(v/attr [:registration-district] (v/present))
           ;(v/attr [:entry-number] (v/present))
           ;(v/attr [:administrative-area] (v/present))
           ;(v/attr [:name-of-doctor-certifying] (v/present))
           ;(v/attr [:name-of-informant] (v/present))
           ;(v/attr [:name-of-registrar] (v/present)))
           data))}
      (fn [fork-args]
        [deceased-details-form create|edit (ui/mui-fork-args fork-args)])]]))

(comment
  ; To fill out the form programmatically:
  (do
    (def test-data
      {:relationship "father",
       :forename "Richard",
       :surname "Roe",})
    ;:sex "Male",
    ;:occupation "Politician",
    ;:dod "12/12/1975",
    ;:middlename "J",
    ;:dob "01/01/1905",
    ;:pod "Edimbourg",
    ;:pob "London"},)
    (swap! form-state assoc :values test-data)
    (darbylaw.web.core/mount-root)))
