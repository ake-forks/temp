(ns darbylaw.web.ui.admin
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [ajax.core :as ajax]))

(rf/reg-event-fx ::load-success
  (fn [{:keys [db]} [_ response]]
    (println "success" response)
    {:db (assoc db :cases response)}))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load!
  (fn [_ _]
    {:http-xhrio
     {:method :get
      :uri "http://localhost:8080/api/cases"
      :timeout 8000
      :response-format (ajax/transit-response-format)
      :on-success [::load-success]
      :on-failure [::load-failure]}}))

(rf/reg-sub ::cases
  (fn [db _]
    (:cases db)))

(defn admin-panel []
  (let [cases @(rf/subscribe [::cases])]
    [:div
     [:h1 "Admin panel"]
     (if (nil? cases)
       "Loading cases..."
       [:ul
        (for [{{:keys [surname forename postcode]} :personal-representative}
              cases]
          [:li (str surname ", " forename ". At " postcode)])])]))

(defn panel []
  (rf/dispatch [::load!])
  [admin-panel])

(defmethod routes/panels :admin-panel []
  [panel])
