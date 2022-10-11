(ns darbylaw.web.ui.admin
  (:require [darbylaw.web.routes :as routes]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [ajax.core :as ajax]
            [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]

            [darbylaw.web.events :as events]))

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
      :uri "/api/cases"
      :timeout 8000
      :response-format (ajax/transit-response-format)
      :on-success [::load-success]
      :on-failure [::load-failure]}}))

(rf/reg-sub ::cases
  (fn [db _]
    (:cases db)))

(defn admin-panel []
  (let [cases @(rf/subscribe [::cases])]

    [mui/container

     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :center}
      [mui/typography {:variant :h1} "Cases"]
      [mui/button {:startIcon (r/as-element [ui/icon-add])
                   :onClick #(rf/dispatch [::ui/navigate :create-case])}
       "Create case"]]
     (if (nil? cases)
       "Loading cases..."
       (for [{{:keys [surname forename postcode]} :personal-representative}
             cases]
         [mui/card
          [mui/card-content
           (str surname ", " forename ". At " postcode)]]))]))

(defn panel []
  (rf/dispatch [::load!])
  [admin-panel])

(defmethod routes/panels :admin-panel []
  [panel])
