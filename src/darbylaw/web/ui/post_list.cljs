(ns darbylaw.web.ui.post-list
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]))

(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (assoc db :post-tasks response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load
  (fn [{:keys [db]} _]
    {:db (update db :config/case-view #(or % :card))
     :http-xhrio
     (ui/build-http
       {:method :get
        :uri "/api/post-tasks"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-sub ::post-tasks
  (fn [db]
    (:post-tasks db)))

(defn panel []
  (let [post-tasks @(rf/subscribe [::post-tasks])]
    [mui/container {:max-width :md}
     [mui/button {:onClick #(rf/dispatch [::load])
                  :startIcon (r/as-element [ui/icon-refresh])
                  :variant :outlined
                  :sx {:align :right}}
      "Refresh"]
     [mui/stack {:spacing 1}
      (cond
        (nil? post-tasks) "Loading..."
        (empty? post-tasks) "No mailing tasks"
        :else
        (for [{:keys [case-id bank-id post-state]} post-tasks]
          [mui/card
           [mui/card-content
            [mui/stack {:direction :row
                        :spacing 1}
             [ui/icon-mail-outlined {:sx {:alignSelf :center
                                          :m 1}}]
             [mui/box {:flexGrow 2}
              [mui/typography [:strong "case "] (str case-id)]
              [mui/typography [:strong "bank "] bank-id]]
             [mui/typography {:font-weight :bold}
              (name post-state)]]]]))]]))
