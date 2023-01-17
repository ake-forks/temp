(ns darbylaw.web.ui.case-model
  (:require [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]))

(rf/reg-sub ::case-id
  :<- [::ui/path-params]
  (fn [path-params]
    (:case-id path-params)))

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::current-case-reference
  :<- [::current-case]
  #(:reference %))

(rf/reg-sub ::nickname
  :<- [::current-case]
  #(-> % :personal-representative :forename))

(rf/reg-sub ::relationship
  :<- [::current-case]
  #(-> % :deceased :relationship))

(rf/reg-sub ::fake?
  :<- [::current-case]
  #(:fake %))

(rf/reg-event-fx ::load-case!
  (fn [_ [_ case-id opts]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id)
        :on-success [::load-success opts]
        :on-failure [::load-failure case-id]})}))

(rf/reg-event-fx ::load-success
  (fn [{:keys [db]} [_ {:keys [on-success]} response]]
    (merge
      {:db (assoc db :current-case response)}
      (when on-success
        (assert (vector? on-success))
        {:dispatch (conj on-success response)}))))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ case-id result]]
    (js/console.error "Case load failed" case-id result)))

;; Utility for awaiting for a specific request to be done.

(rf/reg-event-fx ::-notify-case-loaded!
  (fn [_ [_ case-loaded?]]
    (reset! case-loaded? true)
    nil))

(defn await-load-case! []
  (let [case-id @(rf/subscribe [::case-id])
        case-loaded? (r/atom false)]
    (rf/dispatch [::load-case! case-id
                  {:on-success [::-notify-case-loaded! case-loaded?]}])
    case-loaded?))
