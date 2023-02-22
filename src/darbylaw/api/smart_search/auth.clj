(ns darbylaw.api.smart-search.auth
  (:require [clj-commons.digest :as digest]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [darbylaw.config :refer [config]]
            [darbylaw.api.smart-search.client :refer [base-client]])
  (:import [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))


;; >> Config

;; TODO: Add all these to config?
(def company-name "Darby & Darby")

(mount/defstate public-key
  :start (-> config :smart-search :public-key))

;; TODO: Change to service user
(def user-email "osm@juxt.pro")



;; >> API

(defn today
  []
  (.format (LocalDateTime/now)
           (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->company-token
  [public-key]
  (digest/md5 (str public-key (today))))

(defn auth-token []
  (let [{:keys [body] :as resp}
        (base-client {:method :post
                      :path "/auth/token"
                      :body {:company_name company-name
                             :company_token (->company-token public-key)
                             :user_email user-email}})]
    (get-in body [:data :attributes :access_token])))

(def current-token (atom {:token nil :expires-at nil}))

(defn refresh-token []
  (log/info "Refreshing token")
  (let [new-token (auth-token)
        ;; We hardcode this time because the returned `expires_in` is wrong afaik
        new-expires-at (.plusMinutes (LocalDateTime/now) 15)]
    (swap! current-token assoc :token new-token :expires-at new-expires-at)
    new-token))

;; NOTE: I'm aware of a race condition here, suggestions welcome!
;; - Say that the token has expired
;; - Say two processes get here at once
;; - They'll both call refresh-token and each make a call to get a new token
(defn get-token []
  (let [{:keys [token expires-at]} @current-token]
    ;; NOTE: .isAfter checks if `expires-at` is after the input time
    (if (and token expires-at (.isAfter expires-at (LocalDateTime/now)))
      token
      (refresh-token))))



;; >> Middleware
;; All this needs to be in a separate namespace to avoid a circular dependency

(defn wrap-auth [handler]
  "Add an Authorization header to the request
  Possibly making a request to refresh the token"
  (fn [request]
    (-> request
        (update :headers merge {"Authorization" (str "Bearer " (get-token))})
        handler)))
