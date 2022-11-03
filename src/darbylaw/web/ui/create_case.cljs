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
            ["material-ui-phone-number-2$default" :as MuiPhoneNumber]
            [reagent-mui.material.text-field :as mui-text-field]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.util.phone :as phone]
            [darbylaw.web.util.vlad :as v-utils]
            [applied-science.js-interop :as j]
            [kee-frame.core :as kf]))

(defonce form-state (r/atom nil))

(kf/reg-controller :create-case
  {:params (fn [route-data]
             (when (= :create-case (-> route-data :data :name))
               true))
   :start (fn [& _]
            (reset! form-state nil))
   :stop (fn [& _]
           (reset! form-state nil))})

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

(defn title-field [fork-args]
  [form/autocomplete-field fork-args
    {:name :title
     :label "Title"
     :options ["Mr" "Mrs" "Ms" "Mx" "Dr"]
     :inner-config {:required true}}])

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

(defn dob-date-picker [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [mui-date/date-picker
   {:value (get values :dob)
    :onChange #(set-handle-change {:value %
                                   :path [:dob]})
    :renderInput
    (fn [params]
      (r/as-element
        [mui/text-field
         (merge (js->clj params)
           {:name :dob
            :label (let [date-pattern (j/get-in params [:inputProps :placeholder])]
                     (str "Date of Birth (" date-pattern ")"))
            :required true
            :autoComplete :off
            :error (boolean (form/get-error :dob fork-args))
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

(defn phone-field [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [:> MuiPhoneNumber
   {:name :phone
    :value (get values :phone)
    :label "Mobile Phone"
    :required true
    :onChange #(set-handle-change {:value %
                                   :path [:phone]})
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
          (v/attr [:postcode] (v/present)))
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
       :surname "Doe", :postcode "SW1W 0NY", :title "Mr"})
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
