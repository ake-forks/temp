(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [ajax.core :as ajax]
            [reagent-mui.components :as mui]))

(rf/reg-event-fx ::create-case-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (println "success" response)
    {:db (fork/set-submitting db path false)
     :navigate :deceased-details}))

(rf/reg-event-fx ::create-case-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (println "failure" response)
    {:db (fork/set-submitting db path false)}))

(rf/reg-event-fx ::create-case
  (fn [_ [_ {:keys [values] :as fork-params}]]
    {:http-xhrio
     {:method :post
      :uri "/api/case"
      :params {:personal-representative values}
      :timeout 8000
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      :on-success [::create-case-success fork-params]
      :on-failure [::create-case-failure fork-params]}}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ {:keys [path] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :dispatch [::create-case fork-params]}))

(defn handle-change-fn [{:keys [set-handle-change]}]
  (fn [evt _]
    (set-handle-change {:value (.. evt -target -value)
                        :path [(keyword (.. evt -target -name))]})))

(defn title-field [{:keys [values] :as fork-args}]
  [mui/form-control {:required true}
   [mui/input-label {:id :title-select} "Title"]
   [mui/select {:label "Title"
                :labelId :title-select
                :name :title
                :value (:title values)
                :onChange (handle-change-fn fork-args)
                :variant :filled}
    [mui/menu-item {:value "Mr" :key :Mr} "Mr"]
    [mui/menu-item {:value "Mrs" :key :Mrs} "Mrs"]
    [mui/menu-item {:value "Ms" :key :Ms} "Ms"]
    [mui/menu-item {:value "Mx" :key :Mx} "Mx"]
    [mui/menu-item {:value "Dr" :key :Dr} "Dr"]
    [mui/menu-item {:value "Other" :key :Other} "Other"]]])

(defn name-fields [{:keys [values] :as fork-args}]
  [mui/stack {:direction :row
              :spacing 1}
   [mui/text-field {:label "Forename"
                    :required true
                    :placeholder "Please enter your legal name"
                    :name :forename
                    :value (:forename values)
                    :onChange (handle-change-fn fork-args)
                    :full-width true
                    :variant :filled}]
   [mui/text-field {:label "Middle Name(s)"
                    :name :middlename
                    :value (:middlename values)
                    :onChange (handle-change-fn fork-args)
                    :full-width true
                    :variant :filled}]]
  [mui/text-field {:label "Surname"
                   :required true
                   :name :surname
                   :value (:surname values)
                   :onChange (handle-change-fn fork-args)
                   :full-width true
                   :variant :filled}])

(defn dob-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Date of Birth"
                   :required true
                   :helper-text "Please use format DD/MM/YYYY"
                   :name :dob
                   :value (:dob values)
                   :onChange (handle-change-fn fork-args)
                   :full-width true
                   :variant :filled}])

(defn address-fields [{:keys [values] :as fork-args}]
  [:<>
   [mui/stack {:direction :row
               :spacing 1}
    [mui/text-field {:label "Flat"
                     :name :flat
                     :value (:flat values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Building Name/No."
                     :required true
                     :name :building
                     :value (:building values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Street"
                     :required true
                     :full-width true
                     :name :street1
                     :value (:street1 values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]]
   [mui/text-field {:label "Address line 2"
                    :full-width true
                    :name :street2
                    :value (:street2 values)
                    :onChange (handle-change-fn fork-args)
                    :variant :filled}]
   [mui/stack {:direction :row :spacing 1}
    [mui/text-field {:label "Town/City"
                     :required true
                     :full-width true
                     :name :town
                     :value (:town values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Postcode"
                     :required true
                     :name :postcode
                     :value (:postcode values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]]])

(defn phone-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Phone Number"
                   :required true
                   :full-width true
                   :name :phone
                   :value (:phone values)
                   :onChange (handle-change-fn fork-args)
                   :variant :filled}])

(defn email-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Email Address"
                   :required true
                   :full-width true
                   :name :email
                   :value (:email values)
                   :onChange (handle-change-fn fork-args)
                   :variant :filled}])

(defn personal-info-form [{:keys [handle-submit
                                  submitting?]
                           :as fork-args}]
  [:form
   {:on-submit handle-submit}
   [mui/stack {:spacing 1}
    [mui/typography {:variant :h5} "your details"]
    [title-field fork-args]
    [name-fields fork-args]
    [dob-field fork-args]
    [mui/typography {:variant :h5} "your address"]
    [address-fields fork-args]
    [phone-field fork-args]
    [email-field fork-args]
    [mui/button {:variant :contained
                 :type :submit
                 :disabled submitting?}
     "Next"]]])

(rf/reg-sub ::new-case
  (fn [db]
    (:new-case db)))

(defn panel []
  [mui/container {:max-width :md}
   [mui/typography {:variant :h1}
    "get started"]
   [mui/typography {:variant :p}
    "It looks like you need probate.
    Here are some quick questions about you.
    Then we will ask about the deceased and their relationship to you."]
   [fork/form
    {:on-submit #(rf/dispatch [::submit! %])
     :keywordize-keys true
     :prevent-default? true
     :initial-values
     (let [existing (:personal-representative @(rf/subscribe [::new-case]))]
       (or
         existing
         {:title ""}))}
    personal-info-form]])

(defmethod routes/panels :create-case-panel [] [panel])

(comment
  (def test-data {:street1 "Sesame", :email "test@test.com", :forename "John", :building "11", :phone "888999888", :town "Bristol", :surname "Doe", :postcode "SW1W 0NY", :title "Mr", :dob "01/01/1979"}))
