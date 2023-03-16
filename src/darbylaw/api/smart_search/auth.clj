(ns darbylaw.api.smart-search.auth
  (:require [clj-commons.digest :as digest]
            [clojure.tools.logging :as log]
            [darbylaw.api.smart-search.config :as ss-config]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.smart-search.client :refer [base-client]])
  (:import [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))

;; >> API

(defn today
  []
  (.format (LocalDateTime/now)
           (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->company-token
  [public-key]
  (digest/md5 (str public-key (today))))

(defn auth-token [env]
  (let [config (ss-config/get-config env)
        {:keys [body]}
        ((base-client env) {:method :post
                             :path "/auth/token"
                             :body {:company_name (:company-name config)
                                    :company_token (->company-token (:public-key config))
                                    :user_email (:service-user config)}})]
    (get-in body [:data :attributes :access_token])))



;; >> Middleware
;; All this needs to be in a separate namespace to avoid a circular dependency

(def token-by-env (atom {}))

(defn get-token [env]
  (get @token-by-env env))

(defn refresh-token [env]
  (log/debug "Refreshing token")
  (swap! token-by-env assoc env (auth-token env)))

(defn add-token [env request]
  (update request :headers merge {"Authorization" (str "Bearer " (get-token env))}))

;; There's technically a race condition here if two requests are being processed at the same time
;; This shouldn't be an issue though because we'll just update the token in place
(defn wrap-auth
  "Add an Authorization header to the request
  Possibly making a request to refresh the token"
  [env handler]
  (fn [request]
    (let [{:keys [status] :as response}
          (handler (add-token env request))]
      (if (= status http/status-401-unauthorized)
        (do
          (refresh-token env)
          ;; We always return here, even if the response is a 401 to prevent loops
          (handler (add-token env request)))
        response))))
