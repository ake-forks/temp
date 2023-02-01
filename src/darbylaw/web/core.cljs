(ns darbylaw.web.core
  (:require
    [re-frame.core :as re-frame]
    [breaking-point.core :as bp]
    [darbylaw.web.views :as views]
    [darbylaw.web.config :as config]
    [day8.re-frame.http-fx]
    [kee-frame.core :as kf]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn start! []
  (kf/start!
    {:routes [["/app/about" :about]
              ["/app/landing" :landing]
              ["/app/onboarding" :decision-tree]
              ["/app/admin" :admin]
              ["/app/create-case" :create-case]
              ["/app/case/:case-id/create-deceased-details" :create-deceased-details]
              ["/app/case/:case-id" :dashboard]
              ["/app/case/:case-id/history" :case-history]
              ["/app/case/:case-id/user-details" :user-details]
              ["/app/case/:case-id/deceased-details" :deceased-details]
              ["/app/case/:case-id/view-bank/:bank-id" :view-bank]
              ["/app/case/:case-id/bank-confirmation/:bank-id" :bank-confirmation]]
     :root-component [views/main-panel]
     :log (merge {:level :debug}
            (when-not config/debug?
              {:ns-blacklist ["kee-frame.event-logger"]}))}))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (start!))

(defn init []
  (re-frame/dispatch-sync [::bp/set-breakpoints
                           {:breakpoints [:mobile
                                          768
                                          :tablet
                                          992
                                          :small-monitor
                                          1200
                                          :large-monitor]
                            :debounce-ms 166}])
  (dev-setup)
  (mount-root))
