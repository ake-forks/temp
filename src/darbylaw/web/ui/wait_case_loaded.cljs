(ns darbylaw.web.ui.wait-case-loaded
  "Exposes ::loaded? subscription, which allows for forms
  to initialize with data of latest request, even when
  current-case has been loaded before."
  (:require [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-fx ::loaded
  (fn [{:keys [db]} [_ route-name case-id]]
    {:db (assoc-in db [::case-loaded? route-name case-id] true)}))

(rf/reg-event-fx ::dispose
  (fn [{:keys [db]} _]
    {:db (dissoc db ::case-loaded?)}))

(kf/reg-controller ::load-case
  {:params (fn [route-data]
             (when-let [route-name (#{:user-details
                                      :deceased-details}
                                    (-> route-data :data :name))]
               [route-name (-> route-data :path-params :case-id)]))
   :start (fn [_ [route-name case-id]]
            [::case-model/load-case! case-id
             {:on-success [::loaded route-name case-id]}])
   :stop [::dispose]})

(rf/reg-sub ::-case-loaded?
  (fn [db _]
    (::case-loaded? db)))

(rf/reg-sub ::-route
  (fn [db _]
    (:kee-frame/route db)))

(rf/reg-sub ::loaded?
  :<- [::-case-loaded?]
  :<- [::-route]
  :<- [::case-model/case-id]
  (fn [[case-loaded? route case-id]]
    (get-in case-loaded? [(-> route :data :name) case-id])))
