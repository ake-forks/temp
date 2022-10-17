(ns darbylaw.handler
  (:require
    [reitit.ring :as ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.dev]
    [reitit.coercion.malli]
    [reitit.coercion]
    [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
    [muuntaja.core :as m]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.response :as r]
    [hiccup.page :as h]
    [xtdb.api :as xt]
    [mount.core :as mount]
    [darbylaw.web.theme :as theme]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [darbylaw.api.case :as api.case]
    [darbylaw.config :as config]))

(defn page [meta-info & body]
  (r/response
    (h/html5 {:lang "en"}
      (into
        [:head
         [:title (get meta-info :title "Shadow Full Stack")]
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
        theme/html-header-additions)
      (into
        [:body
         [:noscript "Please enable JavaScript to continue."]]
        body))))

(defn spa [_]
  (page {:title "shadow-cljs Full Stack - App"}
    [:div#app]
    (h/include-js "/js/compiled/app.js")))

(def muuntaja-instance
  (m/create (-> m/default-options
              (assoc-in [:formats "application/transit+json"
                         :encoder-opts :verbose] true))))

(defn do-healthcheck [{:keys [xtdb-node] :as req}]
  (let [xtdb-status (xt/status xtdb-node)]
    {:status 200
     :body {:ip (:remote-addr req)
            :xtdb-node xtdb-status}}))

(defn wrap-xtdb-node [handler]
  (fn [req]
    (handler (-> req
               (assoc :xtdb-node xtdb-node)))))

(defn routes []
  [["/" {:get (fn [_req] (r/redirect "/app/admin"))}]
   ["/app" {:get (fn [_req] (r/redirect "/app/admin"))}]
   ["/app{*path}" {:get spa}]
   [""
    {:middleware [wrap-xtdb-node]}
    ["/healthcheck" {:get do-healthcheck}]
    ["/api"
     (api.case/routes)]]])

(defn authenticated?
  [username password]
  (let [auth (get-in config/config [:web-server :auth])]
    (if (= auth :none)
      true
      (and (contains? auth username)
           (= (get auth username) password)))))

(defn create-auth-middleware
  [handler authenticated?]
  (let [auth (get-in config/config [:web-server :auth])]
    (if (= auth :none)
      handler
      (wrap-basic-authentication handler authenticated?))))

(defn make-router []
  (ring/router
    (routes)
    {; This is recommended in reitit docs, but it breaks the router (why?)
     ;:compile reitit.coercion/compile-request-coercers

     ; Useful for debugging the middleware chain
     ;:reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs

     :data {:coercion reitit.coercion.malli/coercion
            :muuntaja muuntaja-instance
            :middleware [[create-auth-middleware authenticated?]
                         [wrap-cors
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-methods [:get :put :post :delete]]
                         parameters/parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         coercion/coerce-exceptions-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}}))

(comment
  (->> (reitit.core/match-by-path (make-router nil) "/api/case")
    (clojure.walk/prewalk #(cond-> %
                             (map? %) (dissoc :middleware)))))

(defn make-ring-handler []
  (ring/ring-handler
    (make-router)
    (ring/routes
      (ring/redirect-trailing-slash-handler) ; TODO: this is not working?
      (ring/create-resource-handler {:path "/" :root "/public"})
      (ring/create-default-handler))))

(mount/defstate ring-handler
  :start (make-ring-handler))
