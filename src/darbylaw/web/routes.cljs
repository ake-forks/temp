(ns darbylaw.web.routes
  (:require
    [bidi.bidi :as bidi]
    [pushy.core :as pushy]
    [re-frame.core :as rf]))

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])

(def routes
  (atom
    ["/app" {"/about" :about
             "/create-case" :create-case
             ["/case/" :case-id] {"" :dashboard
                                  "/deceased-details" :deceased-details}
             "/admin" :admin
             ["/case/" :case-id "/view-bank/" :bank-id] {"" :go-to-bank}}]))







(defn parse [url]
  (bidi/match-route @routes url))

(defn url-for [& args]
  (apply bidi/path-for (into [@routes] args)))

(comment
  (bidi/path-for @routes :deceased-details :case-id 1)
  (bidi/path-for @routes :deceased-details {:case-id (random-uuid)})
  (url-for :deceased-details {:case-id (random-uuid)})
  (bidi/path-for @routes :create-case nil))

(rf/reg-event-fx ::set-active-panel
  (fn [{:keys [db]} [_ active-panel route-params]]
    {:db (assoc db :active-panel active-panel
                   :route-params route-params)}))

(rf/reg-sub ::active-panel
  (fn [db _]
    (:active-panel db)))

(rf/reg-sub ::route-params
  (fn [db _]
    (:route-params db)))

(defn dispatch [route]
  (let [panel (keyword (str (name (:handler route)) "-panel"))]
    (rf/dispatch [::set-active-panel panel (:route-params route)])))

(defonce history
  (pushy/pushy dispatch parse))

(defn navigate! [route-name params]
  (pushy/set-token! history (url-for route-name params)))

(defn navigate-replacing! [route-name params]
  (pushy/replace-token! history (url-for route-name params)))

(defn start! []
  (pushy/start! history))
