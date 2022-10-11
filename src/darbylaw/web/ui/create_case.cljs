(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]))

(rf/reg-event-fx ::create-case-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (assert (:id response))
    {:db (fork/set-submitting db path false)
     ::ui/navigate-no-history [:deceased-details {:case-id (:id response)}]}))

(rf/reg-event-fx ::create-case-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}))

(rf/reg-event-fx ::create-case
  (fn [_ [_ {:keys [values] :as fork-params}]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri "/api/case"
        :params {:personal-representative values}
        :on-success [::create-case-success fork-params]
        :on-failure [::create-case-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ {:keys [path] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :dispatch [::create-case fork-params]}))

(defn title-field [{:keys [values] :as fork-args}]
  [mui/form-control {:required true}
   [mui/input-label {:id :title-select} "Title"]
   [mui/select {:label "Title"
                :labelId :title-select
                :name :title
                :value (:title values)
                :onChange (ui/form-handle-change-fn fork-args)
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
                    :onChange (ui/form-handle-change-fn fork-args)
                    :full-width true
                    :variant :filled}]
   [mui/text-field {:label "Middle Name(s)"
                    :name :middlename
                    :value (:middlename values)
                    :onChange (ui/form-handle-change-fn fork-args)
                    :full-width true
                    :variant :filled}]]
  [mui/text-field {:label "Surname"
                   :required true
                   :name :surname
                   :value (:surname values)
                   :onChange (ui/form-handle-change-fn fork-args)
                   :full-width true
                   :variant :filled}])

(defn dob-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Date of Birth"
                   :required true
                   :helper-text "Please use format DD/MM/YYYY"
                   :name :dob
                   :value (:dob values)
                   :onChange (ui/form-handle-change-fn fork-args)
                   :full-width true
                   :variant :filled}])

(defn address-fields [{:keys [values] :as fork-args}]
  [:<>
   [mui/stack {:direction :row
               :spacing 1}
    [mui/text-field {:label "Flat"
                     :name :flat
                     :value (:flat values)
                     :onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Building Name/No."
                     :required true
                     :name :building
                     :value (:building values)
                     :onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Street"
                     :required true
                     :full-width true
                     :name :street1
                     :value (:street1 values)
                     :onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]]
   [mui/text-field {:label "Address line 2"
                    :full-width true
                    :name :street2
                    :value (:street2 values)
                    :onChange (ui/form-handle-change-fn fork-args)
                    :variant :filled}]
   [mui/stack {:direction :row :spacing 1}
    [mui/text-field {:label "Town/City"
                     :required true
                     :full-width true
                     :name :town
                     :value (:town values)
                     :onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]
    [mui/text-field {:label "Postcode"
                     :required true
                     :name :postcode
                     :value (:postcode values)
                     :onChange (ui/form-handle-change-fn fork-args)
                     :variant :filled}]]])

(defn phone-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Phone Number"
                   :required true
                   :full-width true
                   :name :phone
                   :value (:phone values)
                   :onChange (ui/form-handle-change-fn fork-args)
                   :variant :filled}])

(defn email-field [{:keys [values] :as fork-args}]
  [mui/text-field {:label "Email Address"
                   :required true
                   :full-width true
                   :name :email
                   :value (:email values)
                   :onChange (ui/form-handle-change-fn fork-args)
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
    [ui/loading-button {:variant :contained
                        :type :submit
                        :loading submitting?}
     "Next"]]])

(defonce form-state (r/atom nil))

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
     :on-submit #(rf/dispatch [::submit! %])
     :keywordize-keys true
     :prevent-default? true
     :initial-values {:title ""}}
    (fn [fork-args]
      [personal-info-form fork-args])]])

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
