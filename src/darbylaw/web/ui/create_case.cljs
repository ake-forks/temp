(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [ajax.core :as ajax]
            [reagent-mui.components :as mui]
            ))

(rf/reg-event-fx ::create-case-success
  (fn [_ [_ response]]
    (println "success" response)))

(rf/reg-event-fx ::create-case-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::create-case
  (fn [_ [_ params]]
    {:http-xhrio
     {:method :post
      :uri "http://localhost:8080/api/case"
      :params params
      :timeout 8000
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      :on-success [::create-case-success]
      :on-failure [::create-case-failure]}}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ {:keys [values path]}]]
    {:db (fork/set-submitting db path true)
     :dispatch [::create-case {:personal-representative values}]}))

(defn handle-change-fn [{:keys [set-handle-change]}]
  (fn [evt _]
    (set-handle-change {:value (.. evt -target -value)
                        :path [(keyword (.. evt -target -name))]})))

(defn personal-info-form [{:keys [handle-submit
                                  submitting?
                                  values]
                           :as fork-args}]
  [:form
   {:on-submit handle-submit}
   [mui/stack {:spacing 1}                                  ;top
    [mui/typography {:variant :h5} "your details"]

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
      [mui/menu-item {:value "Other" :key :Other} "Other"]]
     ]

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
                      :variant :filled}]
     ]
    [mui/text-field {:label "Surname"
                     :required true
                     :name :surname
                     :value (:surname values)
                     :onChange (handle-change-fn fork-args)
                     :full-width true
                     :variant :filled
                     }]

    [mui/input-label {:id :dob-input} "Date of Birth"]
    [mui/text-field {:label "DD/MM/YYYY"
                     :labelId :dob-input
                     :required true
                     :name :dob
                     :value (:dob values)
                     :onChange (handle-change-fn fork-args)
                     :full-width true
                     :variant :filled}]



    [mui/stack {:direction :row :spacing 1 :style {:margin-top "2rem"}}

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
                      :variant :filled}]]
    [mui/text-field {:label "Telephone"
                     :required true
                     :full-width true
                     :name :town
                     :value (:town values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Email Address"
                     :required true
                     :full-width true
                     :name :town
                     :value (:town values)
                     :onChange (handle-change-fn fork-args)
                     :variant :filled}]



    [mui/button {:variant :contained
                 :type :submit
                 :disabled submitting?}
     "Next"]]])

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
     :initial-values {:title ""}}
    personal-info-form]])

(defmethod routes/panels :create-case-panel [] [panel])
