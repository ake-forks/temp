(ns darbylaw.api.smart-search.client
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [malli.core :as m]
            [malli.error :as me]))


;; >> Middleware
;; Why use this?
;; - Less repetition
;; - Composable
;; Why not use this?
;; - Ring style middleware can be confusing
;; - Async is harder, but we're not using it 🤷
;;   - Can also look into interceptors if we need that and still want this style

(defn wrap-base-headers [handler]
  "Headers required by SmartSearch
  https://sandbox-api.smartsearchsecure.com/#section/Requests/Headers"
  (fn [request]
    (-> request
        (update :headers merge {"Accept" "application/json"
                                "Accept-Version" 2})
        handler)))

(defn wrap-ensure-success [handler]
  "Throw an exception if the response is not 200"
  (fn [request]
    (let [{:keys [status body] :as response} (handler request)]
      (when-not (= 200 status)
        (log/error "Request failed" (:errors body))
        (throw (ex-info "Request failed" {:response response})))
      response)))

;; TODO: Check content-type header?
(defn wrap-response-body [handler]
  "Parse the response body as JSON
  If the parse fails throws an exception"
  (fn [request]
    (let [{:keys [status body] :as response} (handler request)]
      (try
        (update response :body #(json/read-str % :key-fn keyword))
        (catch Exception e
          (throw (ex-info "Failed to parse response" {:status status :body body})))))))

(defn wrap-request-body [handler]
  "If the request has a body then write it as a json string
  Also adds the Content-Type header"
  (fn [request]
    (if (nil? (:body request))
      (handler request)
      (-> request
          (update :body json/write-str)
          (update :headers merge {"Content-Type" "application/json"})
          handler))))

;; TODO: Make base-url configurable  
(def base-url "https://sandbox-api.smartsearchsecure.com")
(defn wrap-base-url [handler]
  "If the user specifies a :path then add the base-url to the beginning at :url"
  (fn [request]
    (if-let [path (:path request)]
      (-> request
          (dissoc :path)
          (assoc :url (str base-url path))
          handler)
      (handler request))))



;; >> Client

(defn apply-middleware [handler middleware]
  "Given ha handler and a list of middleware, apply the middleware to the handler"
  (reduce (fn [h m] (m h))
          handler
          middleware))

(defn http-kit-client [r]
  "A client that uses http-kit"
  @(http/request r))

(def base-client
  "An un-authenticated client"
  (apply-middleware
    http-kit-client
    [wrap-base-url
     wrap-base-headers
     wrap-response-body
     wrap-request-body]))
