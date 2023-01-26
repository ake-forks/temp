(ns darbylaw.handler
  (:require
    [reitit.ring :as ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.ring.middleware.multipart :as middleware-multipart]
    [ring.middleware.multipart-params :as ring-middleware-multipart]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.dev]
    [reitit.coercion.malli]
    [reitit.coercion]
    [muuntaja.core :as m]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.not-modified :refer [wrap-not-modified]]
    [ring.util.response :as r]
    [hiccup.page :as h]
    [xtdb.api :as xt]
    [mount.core :as mount]
    [darbylaw.web.theme :as theme]
    [darbylaw.api.settings :as settings-api]
    [darbylaw.api.case :as case-api]
    [darbylaw.api.bank :as bank-api]
    [darbylaw.api.buildingsociety :as buildsoc-api]
    [darbylaw.api.funeral :as funeral-api]
    [darbylaw.api.bank-notification :as bank-notification-api]
    [darbylaw.api.bank-notification.mailing-controls :as mailing]
    [darbylaw.middleware.xtdb :refer [wrap-xtdb-node]]
    [darbylaw.middleware.auth :refer [create-auth-middleware add-user-info authenticated?]]))

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
  (page {:title "Probate Tree"}
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

(defn routes []
  [["/healthcheck" {:middleware [wrap-xtdb-node]
                    :get do-healthcheck}]
   [""
    {:middleware [[create-auth-middleware authenticated?]
                  add-user-info]}
    ["/" {:get (fn [_req] (r/redirect "/app/admin"))}]
    ["/app" {:get (fn [_req] (r/redirect "/app/admin"))}]
    ["/app{*path}" {:get spa}]
    ["/api" {:middleware [wrap-xtdb-node]}
     (settings-api/routes)
     (case-api/routes)
     (bank-api/routes)
     (buildsoc-api/routes)
     (funeral-api/routes)
     (bank-notification-api/routes)
     (mailing/routes)]]])

(defn make-router []
  (ring/router
    (routes)
    {; This is recommended in reitit docs, but it breaks the router (why?)
     ;:compile reitit.coercion/compile-request-coercers

     ; Useful for debugging the middleware chain
     ;:reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs

     :data {:coercion reitit.coercion.malli/coercion
            :muuntaja muuntaja-instance
            :middleware [[wrap-cors
                          :access-control-allow-origin [#".*"]
                          :access-control-allow-methods [:get :put :post :delete :patch]]
                         parameters/parameters-middleware
                         #_middleware-multipart/multipart-middleware
                         ring-middleware-multipart/wrap-multipart-params
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         coercion/coerce-exceptions-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}}))

(comment
  (->> (reitit.core/match-by-path (make-router) "/api/case/1234/buildsoc/4321/generate-notification-letter")
    (clojure.walk/prewalk #(cond-> %
                             (map? %) (dissoc :middleware)))))

(defn no-cache-response
  "Adds headers to enable caching of the request if the response is a success."
  [response]
  (if (= (:status response) 200)
    (-> response
        (r/header "Cache-Control" "no-cache") ;; must-revalidate
        (r/header "Pragma" "no-cache")
        (r/header "Expires" "0"))
    response))

(defn wrap-no-cache [handler]
  "Add the appropriate headers to tell the browser to not *permanently* cache the results of this request.
  
  NOTE: This doesn't mean that the browser won't cache the result.
        It just means that the browser will check to see if it should update the cache.
        i.e. It will send a GET request to the server.
             Which might respond with 304, or the new content. See wrap-not-modified."
  (fn
    ([request]
     (-> request handler no-cache-response))
    ([request respond raise]
     (handler request
              (fn [response] (respond (no-cache-response response)))
              raise))))

(defn make-ring-handler []
  (ring/ring-handler
    (make-router)
    (ring/routes
      (ring/redirect-trailing-slash-handler) ; TODO: this is not working?
      (-> (ring/create-resource-handler {:path "/" :root "/public"})
          (wrap-not-modified)
          (wrap-no-cache))
      (ring/create-default-handler))))

(mount/defstate ring-handler
  :start (make-ring-handler))
