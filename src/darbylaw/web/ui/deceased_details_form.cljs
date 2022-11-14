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
            [darbylaw.web.util.dayjs :as dayjs]
            [clojure.string :as str]))

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
    (update :date-of-birth dayjs/format-date-for-store)
    (->>
      (remove (comp str/blank? val))
      (into {}))))

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
    :label "Who was the deceased to you?"

    :options relationships
    :inner-config {:required true
                   :placeholder "I'm completing this form on behalf of my late..."}
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

(def section-divider
  [mui/box {:style {:height "1rem"}}
   #_[mui/divider {:sx {:m 0}}]])

(defn deceased-details-form* [create|edit {:keys [dirty] :as fork-args}]
  [:form
   [mui/stack {:spacing 4}
    [mui/stack {:spacing 2 :style {:padding-left "10%" :padding-right "10%"}}
     [relationship-field fork-args]]
    [mui/typography {:variant :p :style {:padding-left "10%" :padding-right "10%"}}
     "Please fill out the below form using the exact details from the death certificate.
     Hover over " [ui/icon-search] " for information on where to find each detail, or " [ui/icon-help-outline] " for more information."]
    [mui/divider {:style {:padding-top "1rem"}}]

    [mui/stack {:spacing 1 :style {:margin-top "1rem"}}
     [mui/stack {:spacing 0.5
                 :direction :row
                 :justify-content :end}
      [form/text-field fork-args
       {:name :certificate-number
        :label "Certificate Number"
        :required true}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:p "Format AAA12321"]
                       [:img {:src "/images/tooltips/cert-number.png"
                              :width "200px"}]])}
       [ui/icon-search]]]
     [mui/stack {:direction :row :spacing 0.5 :justify-content :end}
      [form/text-field fork-args
       {:name :entry-number
        :label "Entry Number"
        :required true}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:img {:src "/images/tooltips/entry-number.png"
                              :width "200px"}]])}
       [ui/icon-search]]]
     (r/as-element section-divider)

     [form/text-field fork-args
      {:name :registration-district
       :label "Registration District"
       :required true}]
     (r/as-element section-divider)

     [form/date-picker fork-args
      {:name :date-of-death
       :inner-config {:label-prefix "Date of Death"
                      :required true}}]
     [form/text-field fork-args
      {:name :place-of-death
       :label "Place of Death"
       :required true}]
     (r/as-element section-divider)

     [mui/stack {:direction :row :spacing 2}
      [mui/stack {:spacing 1 :sx {:width "65%"}}
       [form/text-field fork-args
        {:name :first-name
         :label "First Name"
         :full-width true}]
       [form/text-field fork-args
        {:name :first-name
         :label "Surname"
         :full-width true}]]
      [mui/stack {:spacing 1 :sx {:width "35%"}}
       [mui/stack {:spacing 0.5 :direction :row}
        [form/autocomplete-field fork-args
         {:name :sex
          :label "Legal Sex"
          :options ["female" "male"]
          :inner-config {:required true}
          :disableClearable true
          :full-width true}]
        [mui/tooltip
         {:title "Please refer to the individual's sex as written on the death certificate.
         Non-binary genders are not legally recognised in the UK."}
         [ui/icon-help-outline]]]
       [form/text-field fork-args
        {:name :maiden-name
         :label "Maiden Name (if applicable)"
         :full-width true}]]]
     (r/as-element section-divider)

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
     (r/as-element section-divider)

     [form/text-field fork-args
      {:name :occupation
       :label "Occupation"
       :required true}]
     [form/text-field fork-args
      {:name :address
       :label "Usual Address"
       :required true
       :multiline true
       :rows 2}]
     (r/as-element section-divider)

     [mui/stack {:spacing 0.5 :direction :row}
      [form/text-field fork-args
       {:name :cause-of-death
        :label "Cause of Death"
        :required true
        :full-width true
        :multiline true
        :rows 2}]
      [mui/tooltip
       {:title "We need this information because in rare cases the cause of an individual's death can have implications on the processing of their estate. "}
       [ui/icon-help-outline]]]
     [mui/stack {:spacing 0.5 :direction :row}
      [form/text-field fork-args
       {:name :name-of-doctor-certifying
        :label "Certified By"
        :required true
        :full-width true}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:img {:src "/images/tooltips/certified-by.png"
                              :width "200px"}]])}
       [ui/icon-search]]]
     (r/as-element section-divider)

     [mui/stack {:spacing 0.5 :direction :row :justify-content :end}
      [form/text-field fork-args
       {:name :name-of-registrar
        :label "Name of Registrar"
        :required true}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:img {:src "/images/tooltips/name-of-registrar.png"
                              :width "200px"}]])}
       [ui/icon-search]]]

     [form/submit-button fork-args
      {:button {:text (case create|edit
                        :create "Create Case"
                        :edit "Save")
                :variant :contained
                :size :large
                :disabled (and (= create|edit :edit)
                            (not dirty))}}]]]])

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
    (v/attr [:place-of-birth] (v/present))

    (v/attr [:occupation] (v/present))

    (v/attr [:name-of-informant] (v/present))
    (v/attr [:cause-of-death] (v/present))
    (v/attr [:name-of-doctor-certifying] (v/present))
    (v/attr [:name-of-registrar] (v/present))))

(defn deceased-details-form [create|edit {:keys [initial-values]}]
  (r/with-let []
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
         [deceased-details-form* create|edit (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))

(defn dev-auto-fill []
  "Fill out the form programmatically.
  For development purposes only."
  (let [test-data {:forename "forename",
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
                   :administrative-area "parish"}]
    (swap! form-state assoc :values test-data)))

(comment
  (do
    (def test-data)
    (darbylaw.web.core/mount-root)))
