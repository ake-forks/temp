(ns darbylaw.web.views
  (:require
    [re-frame.core :as re-frame]
    [breaking-point.core :as bp]

    [darbylaw.web.events :as events]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.subs :as subs]

    [darbylaw.web.ui.create-case]
    [darbylaw.web.ui.admin]

    [darbylaw.web.semantic :as s]))




;; home

(defn home-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1

      (str "Hello from " @name ". This is the Home Page.")]

     [:div
      [:a {:on-click #(re-frame/dispatch [::events/navigate :dashboard])}
       "go to Dashboard Page"]]

     [:div
      [:a {:on-click #(re-frame/dispatch [::events/navigate :semantic-ui])}
       "go to Semantic Page"]]
     [:div
      [:h3 (str "screen-width: " @(re-frame/subscribe [::bp/screen-width]))]
      [:h3 (str "screen: " @(re-frame/subscribe [::bp/screen]))]]
     [:button
      {:onClick #(re-frame/dispatch [::events/navigate :create-case])}
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










;;antd




;;semantic

(defn semantic-ui-panel []
  [:body
   [s/get-started]])





(defmethod routes/panels :semantic-ui-panel [] [semantic-ui-panel])


;; main

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    (routes/panels @active-panel)))
