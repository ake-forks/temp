(ns darbylaw.web.views
  (:require
   [re-frame.core :as re-frame]
   [breaking-point.core :as bp]
   [darbylaw.web.styles :as styles]
   [darbylaw.web.events :as events]
   [darbylaw.web.routes :as routes]
   [darbylaw.web.subs :as subs]
   [ajax.core :as ajax]))


(re-frame/reg-event-fx ::create-case-success
  (fn [_ _]
    (println "success")))

(re-frame/reg-event-fx ::create-case-success
  (fn [_ _]
    (println "failure")))

(re-frame/reg-event-fx ::create-case
  (fn [_ _]
    {:http-xhrio
     {:method :post
      :uri "http://localhost:8080/api/case"
      :timeout 8000
      :format (ajax/json-request-format)
      :response-format (ajax/json-response-format {:keywords? true})
      :on-success [::create-case-success]
      :on-failure [::create-case-failure]}}))

;; home

(defn home-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1
      {:class (styles/level1)}
      (str "Hello from " @name ". This is the Home Page.")]

     [:div
      [:a {:on-click #(re-frame/dispatch [::events/navigate :about])}
       "go to About Page"]]
     [:div
      [:h3 (str "screen-width: " @(re-frame/subscribe [::bp/screen-width]))]
      [:h3 (str "screen: " @(re-frame/subscribe [::bp/screen]))]]
     [:button
      {:onClick #(re-frame/dispatch [::create-case])}
      "Create case"]]))


(defmethod routes/panels :home-panel [] [home-panel])

;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:on-click #(re-frame/dispatch [::events/navigate :home])}
     "go to Home Page"]]])

(defmethod routes/panels :about-panel [] [about-panel])

;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (routes/panels @active-panel)))
