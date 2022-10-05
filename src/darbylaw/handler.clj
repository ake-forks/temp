(ns darbylaw.handler
  (:require
    ;; third party libs
    [reitit.ring :as ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.dev]
    [reitit.coercion.malli]
    [reitit.coercion]
    [muuntaja.core :as m]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.response :as r]
    [hiccup.page :as h]
    [xtdb.api :as xt]
    [mount.core :as mount]

    ;; app specific
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [darbylaw.api.case :as api.case]))

(defn page [meta-info & body]
  (r/response
    (h/html5 {:lang "en"}
      [:head
       [:title (get meta-info :title "Shadow Full Stack")]
       [:meta {:charset "UTF-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
       [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "true"}]
       [:link {:href "https://fonts.googleapis.com/css2?family=Lexend:wght@300;600&display=swap" :rel "stylesheet"}]
       [:link {:rel "stylesheet" :href "/antd.css"}]]
       ;[:link {:rel "stylesheet" :href "//cdn.jsdelivr.net/npm/semantic-ui@2.4.2/dist/semantic.min.css"}]

      (into
        [:body
         [:noscript "Please enable JavaScript to continue."]]
        body))))


(defn spa [_]
  (page {:title "shadow-cljs Full Stack - App"}
    [:div#app]
    (h/include-js "/js/compiled/app.js")))

;; create muuntaja instance
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
  (println "creating routes")
  [["/" {:get (fn [_req] (r/redirect "/app"))}]

   ["/app{*path}" {:get spa}]

   [""
    {:middleware [wrap-xtdb-node]}
    ["/healthcheck" {:get do-healthcheck}]
    ; Kept in case current healthcheck is set up to reach the `/ip` endpoint:
    ; TODO: remove
    ["/ip" {:get do-healthcheck}]]

   ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                   :responses {200 {:body {:total int?}}}
                   :handler (fn [{{{:keys [x y]} :query} :parameters}]
                              {:status 200
                               :body {:total (+ x y)}})}}]

   ["/api"
    {:middleware [wrap-xtdb-node]}
    (api.case/routes)]])

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
      (ring/create-resource-handler {:path "/" :root "/public"})
      (ring/create-default-handler))))

(mount/defstate ring-handler
  :start (make-ring-handler))
