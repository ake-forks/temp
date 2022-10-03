(ns darbylaw.web.ui.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [ajax.core :as ajax]))

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

(defn select-field [fork-args {:keys [label name options]}]
  (let [{:keys [values handle-change]} fork-args]
    [:p
     [:div [:label {:for name} label]]
     [:div [:select {:id name
                     :name name
                     :value (get values name)
                     :on-change handle-change}
            (for [option options]
              [:option {:value (key option)} (val option)])]]]))

(defn text-field [fork-args {:keys [label name]}]
  (let [{:keys [values handle-change]} fork-args]
    [:p
     [:div [:label {:for name} label]]
     [:div [:input {:id name
                    :name name
                    :value (get values name)
                    :on-change handle-change}]]]))

(defn personal-info-form [{:keys [handle-submit
                                  submitting?]
                           :as fork-args}]
  [:form
   {:on-submit handle-submit}
   [select-field fork-args {:name :title, :label "Title"
                            :options {:mr "Mr"
                                      :mrs "Mrs"
                                      :none "None"}}]
   [text-field fork-args {:name :surname, :label "Surname"}]
   [text-field fork-args {:name :forename, :label "Forenames"}]
   [text-field fork-args {:name :postcode, :label "Postcode"}]
   [:p
    [:button {:type "submit"
              :disabled submitting?}
     "Submit"]]])

(defn panel []
  [:div
   [:h1 "client personal info"]
   [fork/form
    {:on-submit #(rf/dispatch [::submit! %])
     :keywordize-keys true
     :prevent-default? true}
    personal-info-form]])

(defmethod routes/panels :create-case-panel [] [panel])
