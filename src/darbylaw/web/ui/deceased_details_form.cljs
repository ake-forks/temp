(ns darbylaw.web.ui.deceased-details-form
  (:require [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.vlad :as v-utils]
            [reagent.core :as r]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.util.dayjs :as dayjs]
            [clojure.string :as str]
            [darbylaw.web.ui.deceased-details-autofill :as textract]))

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

(def section-divider
  [mui/box {:style {:height "1rem"}}])

(defn textfield-with-tooltip [{:keys [full-width] :as props} tooltip-img fork-args]
  [mui/stack {:direction :row :spacing 0.5 :justify-content :end}
   [form/text-field fork-args
    (if full-width
      (merge props {:style {:width "98%"}})
      props)]
   [mui/tooltip
    {:title
     (r/as-element [mui/box
                    [:img {:src (str "/images/tooltips/" tooltip-img)
                           :width "200px"}]])}
    [ui/icon-search]]])

(defn autofill-button [{:keys [set-handle-change] :as _fork-args}]
  (let [case-id (<< ::case-model/case-id)]
    [mui/button {:onClick #(rf/dispatch [::textract/autofill case-id set-handle-change])
                 :variant :contained
                 :startIcon (r/as-element [ui/icon-manage-search])
                 :sx {:mb 1
                      :mr 1}}
     "autofill from certificate scan"]))

(defn deceased-details-form* [create|edit {:keys [dirty] :as fork-args}]
  [:form
   [mui/stack {:spacing 4}
    [mui/stack {:spacing 2}
     [relationship-field fork-args]]
    [mui/typography {:variant :p}
     "Please fill out the form below using the exact details as printed in the death certificate.
     We need an accurate duplication because the death certificate is a legal document, and ensuring that all
     fields are correct now will make the rest of the probate process smoother."]
    [mui/typography {:variant :p}
     "Hover over " [ui/icon-search] " to see where you will find each field on the death certificate."]
    (when (= :edit create|edit)
      [autofill-button fork-args])
    [mui/divider {:style {:margin-top "0.5rem"}}]
    [mui/stack {:spacing 1 :style {:margin-top "1rem"}}
     (textfield-with-tooltip
       {:name :certificate-number
        :label "Certificate Number"
        :required true}
       "cert-number.png"
       fork-args)
     (textfield-with-tooltip
       {:name :entry-number
        :label "Entry Number"
        :required true}
       "entry-number.png"
       fork-args)
     (r/as-element section-divider)
     (textfield-with-tooltip
       {:name :registration-district
        :label "Registration District"
        :required true
        :full-width true}
       "reg-district.png" fork-args)
     (r/as-element section-divider)
     [mui/stack {:direction :row :spacing 0.5 :justify-content :end}
      [form/date-picker fork-args
       {:name :date-of-death
        :disableOpenPicker true
        :inner-config {:label-prefix "Date of Death"
                       :required true
                       :full-width true}}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:img {:src "/images/tooltips/dod.png"
                              :width "200px"}]])}
       [ui/icon-search]]]
     (textfield-with-tooltip
       {:name :place-of-death
        :label "Place of Death"
        :required true
        :full-width true}
       "pod.png"
       fork-args)
     (r/as-element section-divider)
     [mui/stack {:direction :row :spacing 2}
      [mui/stack {:spacing 1 :sx {:width "65%"}}
       (textfield-with-tooltip
         {:name :forename
          :label "First Name"
          :required true
          :full-width true}
         "forename.png" fork-args)
       (textfield-with-tooltip
         {:name :surname
          :label "Surname"
          :required true
          :full-width true}
         "surname.png" fork-args)]
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
         {:title
          (r/as-element [mui/box
                         [:img {:src "/images/tooltips/sex.png"
                                :width "200px"}]])}
         [ui/icon-search]]]
       (textfield-with-tooltip
         {:name :maiden-name
          :label "Maiden Name (if applicable)"
          :full-width true}
         "maiden.png"
         fork-args)]]
     (r/as-element section-divider)
     [mui/stack {:direction :row :spacing 0.5 :justify-content :end}
      [form/date-picker fork-args
       {:name :date-of-birth
        :disableOpenPicker true
        :inner-config {:label-prefix "Date of Birth"
                       :required true
                       :full-width true}}]
      [mui/tooltip
       {:title
        (r/as-element [mui/box
                       [:img {:src "/images/tooltips/dob.png"
                              :width "200px"}]])}
       [ui/icon-search]]]
     (textfield-with-tooltip
       {:name :place-of-birth
        :label "Place of Birth"
        :required true
        :full-width true}
       "pob.png"
       fork-args)
     (r/as-element section-divider)
     (textfield-with-tooltip
       {:name :occupation
        :label "Occupation"
        :required true
        :full-width true}
       "occupation.png"
       fork-args)
     (textfield-with-tooltip
       {:name :address
        :label "Usual Address"
        :required true
        :full-width true
        :multiline true
        :rows 5}
       "address.png"
       fork-args)
     (r/as-element section-divider)
     (textfield-with-tooltip
       {:name :name-of-informant
        :label "Name of Informant"
        :required true
        :full-width true}
       "informant.png"
       fork-args)
     (r/as-element section-divider)
     (textfield-with-tooltip
       {:name :cause-of-death
        :label "Cause of Death"
        :required true
        :full-width true
        :multiline true
        :rows 6}
       "cause.png"
       fork-args)
     (textfield-with-tooltip
       {:name :name-of-doctor-certifying
        :label "Certified By"
        :required true
        :full-width true}
       "certified-by.png"
       fork-args)
     (r/as-element section-divider)
     (textfield-with-tooltip
       {:name :name-of-registrar
        :label "Name of Registrar"
        :required true}
       "name-of-registrar.png"
       fork-args)
     (r/as-element section-divider)
     [form/submit-button fork-args
      {:button {:text (case create|edit
                        :create "Create Case"
                        :edit "Save")
                :variant :contained
                :style {:width "98%"}
                :size :large
                :disabled (and (= create|edit :edit)
                            (not dirty))}}]]]])

(def data-validation
  (v/join
    (v/attr [:relationship] (v/present))
    (v/attr [:certificate-number] (v/present))
    (v/attr [:registration-district] (v/present))
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
    (v/attr [:address] (v/present))
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

(defn dev-auto-fill
  "Fill out the form programmatically.
  For development purposes only."
  []
  (let [test-data {:forename "forename",
                   :sex "female",
                   :certificate-number "certificate-number"
                   :entry-number "entry-number",
                   :date-of-death (dayjs/read "2022-11-05"),
                   :registration-district "registration district",
                   :occupation "occupation",
                   :name-of-informant "informant",
                   :relationship "mother",
                   :surname "surname",
                   :date-of-birth (dayjs/read "1982-01-06"),
                   :cause-of-death "cause of death",
                   :name-of-doctor-certifying "doctor",
                   :name-of-registrar "registrar",
                   :maiden-name "maiden name",
                   :place-of-death "place of death",
                   :place-of-birth "place of birth",
                   :address "123 address"}]

    (swap! form-state assoc
      :values test-data
      :touched (set (keys test-data)))))
