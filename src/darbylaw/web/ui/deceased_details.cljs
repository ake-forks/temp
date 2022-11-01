(ns darbylaw.web.ui.deceased-details
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.vlad :as v-utils]
            [reagent.core :as r]))

(rf/reg-event-fx ::add-to-case-success
  (fn [{:keys [db]} [_ case-id {:keys [path]} response]]
    {:db (fork/set-submitting db path false)
     ::ui/navigate [:case {:case-id case-id}]}))

(rf/reg-event-fx ::add-to-case-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}))

(rf/reg-event-fx ::add-to-case
  (fn [_ [_ case-id {:keys [values] :as fork-params}]]
    {:http-xhrio
     (ui/build-http
       {:method :patch
        :uri (str "/api/case/" case-id)
        :params {:deceased values}
        :on-success [::add-to-case-success case-id fork-params]
        :on-failure [::add-to-case-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id {:keys [path] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :dispatch [::add-to-case case-id fork-params]}))

(def relationships
  ["Mother"
   "Father"
   "Grandmother"
   "Grandfather"
   "Wife"
   "Husband"
   "Sister"
   "Brother"
   "Child"
   "Cousin"
   "Aunt"
   "Uncle"
   "Stepparent"
   "Friend"
   "Other"])

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

(defn deceased-details-form [{:keys [handle-submit submitting?]
                              :as fork-args}]
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
      [form/autocomplete-field fork-args
       {:name :sex
        :label "Legal Sex"
        :options ["Female" "Male"]
        :freeSolo false
        :inner-config {:required true}}]

      [mui/stack {:direction :row :spacing 1}
       [form/date-picker fork-args
        {:name :date-of-birth
         :inner-config {:label-prefix "Date of Birth"
                        :required true}}]

       [form/date-picker fork-args
        {:name :date-of-death
         :inner-config {:label-prefix "Date of Death"
                        :required true}}]]

      [form/text-field fork-args
       {:name :occupation
        :label "Occupation"
        :required true}]

      [form/text-field fork-args
       {:name :place-of-death
        :label "Place of Death"
        :required true}]

      [form/text-field fork-args
       {:name :cause-of-death
        :label "Cause of Death"
        :required true}]

      [mui/typography {:variant :h5}
       "the death certificate"]

      [mui/stack {:direction :row :spacing 2}

       [form/text-field fork-args
        {:name :registration-district
         :label "Registration District"
         :required true
         :sx {:width "65%"}}]

       [form/text-field fork-args
        {:name :entry-number
         :label "Entry Number"
         :required true
         :sx {:width "35%"}}]]

      [form/text-field fork-args
       {:name :administrative-area
        :label "Parish (if specified) and County"
        :required true}]

      [mui/stack {:spacing 1}
       [form/text-field fork-args
        {:name :name-of-doctor-certifying
         :label "Name of Doctor Certifying Death"
         :required true}]

       [form/text-field fork-args
        {:name :name-of-informant
         :label "Name of Informant"
         :required true}]

       [form/text-field fork-args
        {:name :name-of-registrar
         :label "Name of Registrar"
         :required true}]]

      [form/submit-button fork-args
       {:button {:text "Create Case"
                 :variant :contained}}]]]]])

(defonce form-state (r/atom nil))

(defn panel []
  (let [route-params @(rf/subscribe [::ui/path-params])]
    [mui/container {:max-width :md}
     [fork/form
      {:state form-state
       :on-submit (let [case-id (:case-id route-params)]
                    ;; NOTE: Can we use a kf/reg-controller to ensure this?
                    ;;       Maybe redirect if not set properly
                    (assert case-id)
                    #(rf/dispatch [::submit! case-id %]))
       :keywordize-keys true
       :prevent-default? true
       :initial-values {:relationship "" :sex ""}
       :validation
       (fn [data]
         (v/field-errors
           (v/join
             (v/attr [:relationship] (v/present))
             (v/attr [:forename] (v/present))
             (v/attr [:surname] (v/present))
             (v/attr [:sex]
                     (v/chain
                       (v/present)
                       (v/one-of #{"Male" "Female"})))
             (v/attr [:date-of-birth] 
                     (v/chain
                       (v-utils/not-nil)
                       (v-utils/valid-dayjs-date)))
             (v/attr [:date-of-death] 
                     (v/chain
                       (v-utils/not-nil)
                       (v-utils/valid-dayjs-date)))
             (v/attr [:occupation] (v/present))
             (v/attr [:place-of-death] (v/present))
             (v/attr [:cause-of-death] (v/present))
             (v/attr [:registration-district] (v/present))
             (v/attr [:entry-number] (v/present))
             (v/attr [:administrative-area] (v/present))
             (v/attr [:name-of-doctor-certifying] (v/present))
             (v/attr [:name-of-informant] (v/present))
             (v/attr [:name-of-registrar] (v/present)))
           data))}
      (fn [fork-args]
        [deceased-details-form (ui/mui-fork-args fork-args)])]]))

(defmethod routes/panels :deceased-details-panel [] [panel])

(comment
  ; To fill out the form programmatically:
  (do
    (def test-data
      {:forename "Richard",
       :sex "Male",
       :occupation "Politician",
       :dod "12/12/1975",
       :relationship "Father",
       :surname "Roe",
       :middlename "J",
       :dob "01/01/1905",
       :pod "Edimbourg",
       :pob "London"},)
    (swap! form-state assoc :values test-data)
    (darbylaw.web.core/mount-root)))
