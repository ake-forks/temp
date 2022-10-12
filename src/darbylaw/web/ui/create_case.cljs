(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [reagent-mui.components :as mui]
            [reagent-mui.x.date-picker :as mui-date]
            [darbylaw.web.ui :as ui]))

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
    (update :dob #(.format % "YYYY-MM-DD"))))

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

(defn common-input-field-props
  [k
   {:keys [values touched handle-change handle-blur errors] :as _fork-args}
   {:keys [error-icon?] :as _options}]
  (let [error (and (touched k)
                (get errors (list k)))]
    (cond-> {:name k
             :value (get values k)
             :onChange handle-change
             :onBlur handle-blur
             :error (boolean error)}
      error-icon? (assoc :InputProps
                          (when error
                            {:endAdornment
                             (r/as-element
                               [mui/input-adornment {:position :end}
                                [ui/icon-error-outline {:color :error}]])})))))

(defn common-text-field-props [k fork-args]
  (common-input-field-props k fork-args {:error-icon? true}))

(defn title-field [fork-args]
  [mui/form-control
   [mui/input-label {:id :title-select} "Title"]
   [mui/select (merge (common-input-field-props :title fork-args {})
                 {:label "Title"
                  :required true
                  :labelId :title-select})
    [mui/menu-item {:value "Mr" :key :Mr} "Mr"]
    [mui/menu-item {:value "Mrs" :key :Mrs} "Mrs"]
    [mui/menu-item {:value "Ms" :key :Ms} "Ms"]
    [mui/menu-item {:value "Mx" :key :Mx} "Mx"]
    [mui/menu-item {:value "Dr" :key :Dr} "Dr"]
    [mui/menu-item {:value "Other" :key :Other} "Other"]]])

(defn name-fields [fork-args]
  [mui/stack {:direction :row
              :spacing 1}
   [mui/text-field (merge (common-text-field-props :forename fork-args)
                     {:label "Forename"
                      :placeholder "Your legal name"
                      :full-width true})]
   [mui/text-field (merge (common-text-field-props :middlename fork-args)
                     {:label "Middle Name(s)"
                      :full-width true})]
   [mui/text-field (merge (common-text-field-props :surname fork-args)
                     {:label "Surname"
                      :full-width true})]])

(defn dob-text-field [fork-args]
  [mui/text-field (merge (common-text-field-props :dob fork-args)
                    {:label "Date of Birth"
                     :required true
                     :helper-text "Please use format DD/MM/YYYY"
                     :full-width true})])

(defn dob-date-picker [{:keys [values set-handle-change handle-blur errors touched] :as _fork-args}]
  [mui-date/date-picker
   {:value (get values :dob)
    :label "Date of Birth"
    :onChange #(set-handle-change {:value %
                                   :path [:dob]})
    :renderInput (fn [params]
                   (r/as-element
                     [mui/text-field
                      (merge (js->clj params)
                        (let [error (and (touched :dob)
                                         (get errors (list :dob)))]
                          {:name :dob
                           :required true
                           :error (boolean error)
                           :onBlur handle-blur}))]))
    ; In case we want to add a helperText with the expected date pattern,
    ; (not working so far, will need some tweaking):
    ; {:helperText (-> params-clj :inputProps :placeholder)}
    :openTo :year
    :views [:year :month :day]
    :full-width true}])

(defn address-fields [{:keys [values] :as _fork-args}]
  [:<>
   [mui/stack {:direction :row
               :spacing 1}
    [mui/text-field {:label "Flat"
                     :name :flat
                     :value (:flat values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Building Name/No."
                     ;:required true
                     :name :building
                     :value (:building values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Street"
                     ;:required true
                     :full-width true
                     :name :street1
                     :value (:street1 values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]]
   [mui/text-field {:label "Address line 2"
                    :full-width true
                    :name :street2
                    :value (:street2 values)
                    ;:onChange (ui/form-handle-change-fn fork-args)
                    :variant :filled}]
   [mui/stack {:direction :row :spacing 1}
    [mui/text-field {:label "Town/City"
                     ;:required true
                     :full-width true
                     :name :town
                     :value (:town values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Postcode"
                     ;:required true
                     :name :postcode
                     :value (:postcode values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]]])

(defn phone-field [{:keys [values] :as _fork-args}]
  [mui/text-field {:label "Phone Number"
                   ;:required true
                   :full-width true
                   :name :phone
                   :value (:phone values)
                   ;:onChange (ui/form-handle-change-fn fork-args)
                   :variant :filled}])

(defn email-field [{:keys [values] :as _fork-args}]
  [mui/text-field {:label "Email Address"
                   ;:required true
                   :full-width true
                   :name :email
                   :value (:email values)
                   ;:onChange (ui/form-handle-change-fn fork-args)
                   :variant :filled}])

(defn personal-info-form [{:keys [handle-submit
                                  submitting?]
                           :as fork-args}]
  [:form
   [mui/stack {:spacing 1}
    [mui/typography {:variant :h5} "your details"]
    [title-field fork-args]
    [name-fields fork-args]
    #_[dob-text-field fork-args]
    [dob-date-picker fork-args]
    [mui/typography {:variant :h5} "your address"]
    [address-fields fork-args]
    [phone-field fork-args]
    [email-field fork-args]
    ; We could be using a button of type "submit", instead of handling onclick,
    ; but then the browser will be reacting to required fields when submitting.
    ; (i.e. Chrome shows a popup on the first required field that is empty).
    [ui/loading-button {:onClick handle-submit
                        :variant :contained
                        :loading submitting?}
     "Next"]]])

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
  (str name " must be a valid date."))

(defn panel []
  [mui/container {:max-width :md}
   [mui/typography {:variant :h1}
    "get started"]
   [mui/typography {:variant :p}
    "It looks like you need probate.
    Here are some quick questions about you.
    Then we will ask about the deceased and their relationship to you."]
   [fork/form
    {:state form-state
     :on-submit #(rf/dispatch [::create-case %])
     :keywordize-keys true
     :prevent-default? true
     :initial-values {:title ""}
     :validation
     (fn [data]
       (v/field-errors
         (v/join
           (v/attr [:title] (v/present))
           (v/attr [:forename] (v/present))
           (v/attr [:surname] (v/present))
           (v/attr [:dob] (v/chain
                            (not-nil)
                            (valid-dayjs-date))))
         data))}
    (fn [fork-args]
      [personal-info-form (ui/mui-fork-args fork-args)])]])

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
  (-> @form-state :values :dob)
  (-> @form-state :values :dob .isValid)
  (-> @form-state :values :dob (.format "YYYY-MM-DD")))
