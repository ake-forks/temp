(ns darbylaw.web.ui.post-list
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]
            [darbylaw.web.util.date :as date-util]
            [darbylaw.web.ui.app-settings :as app-settings]))

(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (assoc db :post-tasks response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load-post-tasks
  (fn [_ _]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri "/api/post-tasks"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-event-fx ::load
  (fn [_ _]
    {:fx [[:dispatch [::load-post-tasks]]
          [:dispatch [::app-settings/load]]]}))

(rf/reg-sub ::post-tasks
  (fn [db]
    (some->> (:post-tasks db)
      (sort-by :created-at >))))

(defn panel []
  (let [post-tasks @(rf/subscribe [::post-tasks])
        settings @(rf/subscribe [::app-settings/settings])]
    [mui/container {:max-width :md}
     [mui/stack {:direction :row
                 :spacing 2}
      [mui/button {:onClick #(rf/dispatch [::load])
                   :startIcon (r/as-element [ui/icon-refresh])
                   :variant :outlined
                   :sx {:align :right
                        :mb 2}}
       (if (nil? post-tasks)
         "Load"
         "Refresh")]
      (when settings
        [mui/form-group
         [mui/form-control-label
          {:control
           (r/as-element
             [mui/switch {:checked (not (:post-letters-disabled? settings))
                          :onChange #(rf/dispatch
                                       [::app-settings/merge-settings
                                        {:post-letters-disabled?
                                         (not (ui/event-target-checked %))}])}])
           :label
           "Post letters in the background"}]])]
     [mui/stack {:spacing 1}
      (cond
        (nil? post-tasks) "Loading..."
        (empty? post-tasks) "No mailing tasks"
        :else
        (for [{:keys [case-id bank-id post-state created-at]} post-tasks]
          ^{:key (pr-str [case-id bank-id])}
          [mui/card
           [mui/card-content
            [mui/stack {:direction :row
                        :spacing 1}
             [ui/icon-mail-outlined {:sx {:alignSelf :center
                                          :m 1}}]
             [mui/box {:flexGrow 2}
              [mui/typography [:strong "case "] (str case-id)]
              [mui/typography [:strong "bank "] bank-id]]
             [mui/box
              [mui/typography {:font-weight :bold
                               :text-align :right}
               (name post-state)]
              [mui/typography {:variant :body2
                               :text-align :right}
               "created: " (date-util/show-local-numeric created-at)]]]]]))]]))
