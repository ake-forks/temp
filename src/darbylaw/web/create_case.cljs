(ns darbylaw.web.create-case
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]))


(rf/reg-sub ::counter
  (fn [db _]
    (:counter db)))

(rf/reg-event-fx ::reset-counter
  (fn [{:keys [db]} _]
    {:db (assoc db :counter 0)}))

(rf/reg-event-fx ::inc-counter
  (fn [{:keys [db]} _]
    {:db (update db :counter inc)}))

(defn panel []
  (rf/dispatch-sync [::reset-counter])
  (let [counter @(rf/subscribe [::counter])]
    [:div
     [:h1 "create case"]
     [:p (str "Counter: " counter)]
     [:button
      {:onClick #(rf/dispatch [::reset-counter])}
      "Reset!"]
     [:button
      {:onClick #(rf/dispatch [::inc-counter])}
      "Inc!"]]))

(defmethod routes/panels :create-case-panel [] [panel])

(comment
  (cljs.pprint/pprint @re-frame.db/app-db))