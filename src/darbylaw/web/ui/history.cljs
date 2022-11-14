(ns darbylaw.web.ui.history
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [kee-frame.core :as kf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]
            [darbylaw.web.ui.error-boundary :refer [error-boundary]]
            [darbylaw.web.ui.diff :as diff]))

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

(rf/reg-event-fx ::load-event-success
  (fn [{:keys [db]} [_ id response]]
    {:db (assoc-in db [:history-event id] response)}))

(rf/reg-event-fx ::load-event-failure
  (fn [_ [_ id result]]
    (js/console.error "Loading event failed!" id result)))

(rf/reg-event-fx ::load-event!
  (fn [_ [_ id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/event/" id)
        :on-success [::load-event-success id]
        :on-failure [::load-event-failure id]})}))

(rf/reg-sub ::history-event
  (fn [db [_ id]]
    (get-in db [:history-event id])))

(defn format-timestamp [d]
  (.toLocaleString d))

(comment
  (format-timestamp (js/Date.)))

(defn row [{:keys [id timestamp event]}]
  (r/with-let [open? (r/atom false)]
    [:<>
     [mui/table-row {:key (str id)}
      [mui/table-cell (format-timestamp timestamp)]
      [mui/table-cell event]
      [mui/table-cell
       [mui/icon-button {:onClick #(do
                                     (swap! open? not)
                                     (rf/dispatch [::load-event! id]))}
        (if @open?
          [ui/icon-keyboard-arrow-up]
          [ui/icon-keyboard-arrow-down])]]]
     [mui/table-row {:key (str "data-" (str id))}
      [mui/table-cell {:colSpan 3
                       :sx {:pb 0 :pt 0}}
       [mui/collapse {:in @open?
                      :unmountOnExit true}
        (let [{:keys [case-before case-after]} @(rf/subscribe [::history-event id])]
          [error-boundary
           [mui/box {:sx {:fontFamily :monospace}}
            (diff/diff case-before case-after)]])]]]]))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        history @(rf/subscribe [::case-history case-id])]
    [mui/container {:max-width :sm
                    :sx {:mt 4 :mb 4}}
     [mui/stack {:spacing 2}
      [mui/typography {:variant :h3}
       (str "case history")]
      [mui/typography {:variant :p}
       "Case id: "
       [mui/box {:component :span
                 :sx {:fontFamily :monospace}} "#" case-id]]
      [mui/table-container ;{:sx {:width :max-content}}
       [mui/table
        [mui/table-body
         (for [{:keys [id] :as history-item} history]
           ^{:key id} [row history-item])]]]]]))

(defmethod routes/panels :case-history-panel [] [panel])
