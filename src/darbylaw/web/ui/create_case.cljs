(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [ajax.core :as ajax]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]))

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
      :uri "/api/case"
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
   [mui/stack {:spacing 1}
    [mui/typography {:variant :h5} "your details"]
    [mui/form-control
     [mui/input-label {:id :title-select} "Title"]
     [mui/select {:label "Title"
                  :labelId :title-select
                  :name :title
                  :value (:title values)
                  :onChange (handle-change-fn fork-args)}
      [mui/menu-item {:value "Mr" :key :Mr} "Mr"]
      [mui/menu-item {:value "Ms" :key :Ms} "Ms"]
      [mui/menu-item {:value "Mx" :key :Mx} "Mx"]
      [mui/menu-item {:value "Dr" :key :Dr} "Dr"]
      [mui/menu-item {:value "Other" :key :Other} "Other"]]
     [ui/???_TO_BE_DEFINED_??? "What should be the options?"]]
    [mui/stack {:direction :row
                :spacing 1}
     [mui/text-field {:label "Forename"
                      :name :forename
                      :value (:forename values)
                      :onChange (handle-change-fn fork-args)
                      :full-width true}]
     [mui/text-field {:label "Surname"
                      :name :surname
                      :value (:surname values)
                      :onChange (handle-change-fn fork-args)
                      :full-width true}]]
    [ui/???_TO_BE_DEFINED_??? "How do we want to split name?
                               (Forename, MiddleName and Surname in .NET code,
                               but Forenames in mockups)."]
    [mui/text-field {:label "Postcode"
                     :name :postcode
                     :value (:postcode values)
                     :onChange (handle-change-fn fork-args)}]
    [mui/button {:variant :contained
                 :type :submit
                 :disabled submitting?}
     "Submit"]]])

(defn panel []
  [mui/container
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
