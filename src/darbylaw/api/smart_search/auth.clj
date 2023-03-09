(ns darbylaw.api.smart-search.auth
  (:require [clj-commons.digest :as digest]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [darbylaw.config :refer [config]]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.smart-search.client :refer [base-client]])
  (:import [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))


;; >> Config

(mount/defstate company-name
  :start (-> config :smart-search :company-name))

(mount/defstate public-key
  :start (-> config :smart-search :public-key))

(mount/defstate user-email
  :start (-> config :smart-search :service-user))



;; >> API

(defn today
  []
  (.format (LocalDateTime/now)
           (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->company-token
  [public-key]
  (digest/md5 (str public-key (today))))

(defn auth-token []
  (let [{:keys [body]}
        (base-client {:method :post
                      :path "/auth/token"
                      :body {:company_name company-name
                             :company_token (->company-token public-key)
                             :user_email user-email}})]
    (get-in body [:data :attributes :access_token])))



;; >> Middleware
;; All this needs to be in a separate namespace to avoid a circular dependency

(def token (atom nil))
(defn add-token [request]
  (update request :headers merge {"Authorization" (str "Bearer " @token)}))

;; There's technically a race condition here if two requests are being processed at the same time
;; This shouldn't be an issue though because we'll just update the token in place
(defn wrap-auth
  "Add an Authorization header to the request
  Possibly making a request to refresh the token"
  [handler]
  (fn [request]
    (let [{:keys [status] :as response}
          (-> request add-token handler)]
      (if (= status http/status-401-unauthorized)
        (do
          (log/info "Refreshing token")
          (reset! token (auth-token))
          ;; We always return here, even if the response is a 401 to prevent loops
          (-> request add-token handler))
        response))))
