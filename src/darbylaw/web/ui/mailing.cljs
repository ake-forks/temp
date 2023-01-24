(ns darbylaw.web.ui.mailing
  (:require [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]
            [darbylaw.web.util.date :as date-util]
            [darbylaw.web.ui.app-settings :as app-settings]
            [darbylaw.api.bank-list :as banks]))

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
        :uri "/api/mailing/items"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-event-fx ::load
  (fn [_ _]
    {:fx [[:dispatch [::load-post-tasks]]
          [:dispatch [::app-settings/load]]]}))

(rf/reg-event-fx ::run-success
  (fn [_ [_ _response]]))

(rf/reg-event-fx ::run-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::run
  (fn [_ _]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri "/api/mailing/run"
        :on-success [::run-success]
        :on-failure [::run-failure]})}))

(rf/reg-event-fx ::sync-success
  (fn [_ [_ _response]]))

(rf/reg-event-fx ::sync-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::sync
  (fn [_ _]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri "/api/mailing/sync"
        :on-success [::sync-success]
        :on-failure [::sync-failure]})}))

(rf/reg-sub ::post-tasks
  (fn [db]
    (some->> (:post-tasks db)
      (sort-by :created-at >))))

(defn manual-controls []
  (let [settings @(rf/subscribe [::app-settings/settings])]
    (when settings
      [mui/accordion
       [mui/accordion-summary {:expandIcon (r/as-element [ui/icon-expand-more])
                               :sx {:flex-direction :row-reverse}}
        [:b "manual controls for mailing"]]
       [mui/accordion-details
        [mui/typography
         [:b "Real"] " letters are sent daily. It can be disabled in case of emergency."]
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
           "Post real letters at 5 PM"}]]
        [mui/typography {:sx {:mt 2}}
         [:b "Fake"] " letters are sent and synced only on-demand."]
        [mui/stack {:direction :row
                    :spacing 2
                    :sx {:mt 1}}
         [mui/button {:onClick #(rf/dispatch [::run])
                      :startIcon (r/as-element [ui/icon-outbox])
                      :variant :outlined
                      :color :error}
          "Upload fake letters now!"]
         [mui/button {:onClick #(rf/dispatch [::sync])
                      :startIcon (r/as-element [ui/icon-cloud-sync])
                      :variant :outlined}
          "Sync state of fake letters"]]]])))


(defn panel []
  (let [post-tasks @(rf/subscribe [::post-tasks])]
    [mui/container {:max-width :md}
     [mui/stack {:direction :row
                 :spacing 2
                 :align-items :flex-start
                 :sx {:mb 2}}
      [mui/button {:onClick #(rf/dispatch [::load])
                   :startIcon (r/as-element [ui/icon-refresh])
                   :variant :outlined}
       (if (nil? post-tasks)
         "Load"
         "Refresh")]]
     [mui/box {:sx {:mb 2}}
      [manual-controls]]
     [mui/stack {:spacing 1}
      (cond
        (nil? post-tasks) [:<>]
        (empty? post-tasks) "No mailing tasks"
        :else
        (for [{case-data :case
               :keys [bank-id
                      review-by review-timestamp send-action
                      upload-state
                      send-state send-error send-state-changed]} post-tasks]
          ^{:key (pr-str [(:id case-data) bank-id])}
          [mui/card
           [mui/card-content
            [mui/stack {:direction :row
                        :spacing 1}
             [ui/icon-mail-outlined {:sx {:alignSelf :center
                                          :m 1}}]
             [mui/box {:flexGrow 2}
              [mui/typography [:strong "type "] "bank notification letter"]
              [mui/tooltip {:title (str "case id: " (:id case-data))}
               [mui/typography [:strong "case "] (:reference case-data)]]
              [mui/typography [:strong "bank "] (or (banks/bank-label bank-id)
                                                    bank-id)]]
             [mui/box
              [mui/typography {:font-weight :bold
                               :text-align :right}
               (cond
                 (some? send-state)
                 (str
                   (case send-state
                     :error "send error"
                     (name send-state))
                   (when (= send-state :error)
                     (str " (" send-error ")"))
                   (when (= send-action :fake-send)
                     " [fake]"))

                 (some? upload-state)
                 (str (name upload-state)
                   (when (= send-action :fake-send)
                     " [fake]"))

                 (some? send-action)
                 (case send-action
                   :send "ready to send"
                   :fake-send "ready to send [fake]"
                   :do-not-send "not to be sent")

                 :else
                 "pending approval")]
              (when (or (some? review-by)
                        (some? review-timestamp))
                [mui/typography {:variant :body2
                                 :text-align :right}
                 "reviewed by " review-by
                 " at "(date-util/show-local-numeric review-timestamp)])

              (when send-state-changed
                [mui/typography {:variant :body2
                                 :text-align :right}
                 "send status changed at " (date-util/show-local-numeric send-state-changed)])]]]]))]]))

