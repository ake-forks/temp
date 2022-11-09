(ns darbylaw.web.ui.history
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [kee-frame.core :as kf]
            [darbylaw.web.ui :as ui]))

(rf/reg-event-fx ::load-success
  (fn [{:keys [db]} [_ case-id response]]
    {:db (assoc-in db [:case-history case-id] response)}))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ case-id result]]
    (js/console.error "Loading case history failed!" case-id result)))

(rf/reg-event-fx ::load!
  (fn [_ [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id "/history")
        :on-success [::load-success case-id]
        :on-failure [::load-failure case-id]})}))

(kf/reg-controller ::load
  {:params (fn [route-data]
             (when (= :case-history (-> route-data :data :name))
               (-> route-data :path-params :case-id)))
   :start (fn [_context case-id]
            [::load! case-id])})

(rf/reg-sub ::case-history
  (fn [db [_ case-id]]
    (get-in db [:case-history case-id])))

(defn format-timestamp [d]
  (.toLocaleString d))

(comment
  (format-timestamp (js/Date.)))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        history @(rf/subscribe [::case-history case-id])]
    [mui/container
     [mui/typography {:variant :h3}
      (str "case history")]
     [mui/typography {:variant :p}
      (str "case #" case-id)]
     [mui/table-container {:sx {:width :max-content}}
      [mui/table
       [mui/table-body
        (for [{:keys [id timestamp event]} history]
          [mui/table-row {:key (str id)}
           [mui/table-cell (format-timestamp timestamp)]
           [mui/table-cell event]])]]]]))

(defmethod routes/panels :case-history-panel [] [panel])

