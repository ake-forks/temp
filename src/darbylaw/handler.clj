(ns darbylaw.handler
  (:require
    ;; third party libs
    [reitit.ring :as ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.coercion.spec :as spec-coercion]
    [muuntaja.core :as m]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.util.response :as r]
    [hiccup.page :as h]
    [xtdb.api :as xt]
    [mount.core :as mount]

    ;; app specific
    [darbylaw.xtdb-node :refer [xtdb-node]]))

(defn page [meta-info & body]
  (r/response
   (h/html5 {:lang "en"}
            [:head
             [:title (get meta-info :title "Shadow Full Stack")]
             [:meta {:charset "UTF-8"}]
             [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
             [:link {:rel "stylesheet" :href "/css/site.css"}]]
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
                (assoc :default-format "application/edn"))))

(defn ip-handler [request]
  {:status 200
   :headers {"content-type" "application/edn"}
   :body {:ip (:remote-addr request)}})

(defn create-case [args]
  (clojure.pprint/pprint (keys args))
  (xt/await-tx xtdb-node
    (xt/submit-tx xtdb-node [[::xt/put {:xt/id :testing
                                        :name "Test"}]]))
  {:status 200
   :body {:result "ok"}})

(def routes
  [["/" {:get spa}]

   ["/ip" {:get ip-handler
           :name ::ip}]

   ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                   :responses {200 {:body {:total int?}}}
                   :handler (fn [{{{:keys [x y]} :query} :parameters}]
                              {:status 200
                               :body {:total (+ x y)}})}}]

   ["/api/case" {:post create-case}]])

(defn make-ring-handler []
  (ring/ring-handler
    (ring/router
      routes
      {:data {:coercion spec-coercion/coercion
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
                           coercion/coerce-response-middleware]}})
    (ring/routes
      (ring/create-resource-handler {:path "/" :root "/public"})
      (ring/create-default-handler))))

; Preserved only for compatibily. To be removed.
(def app)

(mount/defstate ring-handler
  :start (let [handler (make-ring-handler)]
           (alter-var-root (var app) (constantly handler))
           handler))

(comment
  (mount/start))
