(ns darbylaw.api.smart-search.api
  (:require [org.httpkit.client :as http]
            [mount.core :as mount]
            [clojure.data.json :as json]
            [clj-commons.digest :as digest]
            [clojure.tools.logging :as log]
            [darbylaw.config :refer [config]]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.smart-search.data :as ss-data]
            [malli.core :as m]
            [malli.error :as me])
  (:import [java.time.format DateTimeFormatter]
           [java.time LocalDateTime]))


;; >> Config

;; TODO: Add all these to config?
(def company-name "Darby & Darby")

(mount/defstate public-key
  :start (-> config :smart-search :public-key))

;; TODO: Change to service user
(def user-email "osm@juxt.pro")

(def base-url "https://sandbox-api.smartsearchsecure.com")

(def base-headers {"Content-Type" "application/json"
                   "Accept-Version" 2})



;; >> Auth

(defn today
  []
  (.format (LocalDateTime/now)
           (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn ->company-token
  [public-key]
  (digest/md5 (str public-key (today))))

(defn get-auth []
  (let [{:keys [status body]}
        @(http/post
           (str base-url "/auth/token")
           {:headers base-headers
            :body (json/write-str
                    {:company_name company-name
                     :company_token (->company-token public-key)
                     :user_email user-email})})]
    (when-not (= status 200)
      (throw (ex-info "Failed to get auth token" {:status status :body body})))
    (-> body
        (json/read-str :key-fn keyword)
        (get-in [:data :attributes :access_token]))))

(def auth-token (atom {:token nil :expires-at nil}))

(defn refresh-token []
  (log/info "Refreshing token")
  (let [new-token (get-auth)
        ;; We hardcode this time because the returned `expires_in` is wrong afaik
        new-expires-at (.plusMinutes (LocalDateTime/now) 15)]
    (swap! auth-token assoc :token new-token :expires-at new-expires-at)
    new-token))

;; NOTE: I'm aware of a race condition here, suggestions welcome!
;; - Say that the token has expired
;; - Say two processes get here at once
;; - They'll both call refresh-token and each make a call to get a new token
(defn get-token []
  (let [{:keys [token expires-at]} @auth-token]
    ;; NOTE: .isAfter checks if `expires-at` is after the input time
    (if (and token expires-at (.isAfter expires-at (LocalDateTime/now)))
      token
      (refresh-token))))



;; >> API Calls

(defn supported-documents []
  (let [{:keys [status body] :as resp}
        @(http/get (str base-url "/lookup/doccheck/supported-documents")
                   {:body (json/write-str
                            {:country "gbr"})
                    :headers (merge base-headers
                                    {"Authorization"
                                     (str "Bearer " (get-token))})})
        _ (when-not (= 200 status)
            (println status)
            (spit "err.html" body)
            (throw (ex-info "Request failed"
                            {:status status :body body})))]
    (try
      (json/read-str body :key-fn keyword)
      (catch Exception e
        (spit "err.html" body)
        (throw (ex-info "Failed to parse response"
                        {:status status :body body}))))))

(defn smart-doc-check [data]
  (when-not (m/validate ss-data/smart-doc--schema data)
    (-> ss-data/smart-doc--schema
        (m/explain data)
        (malli.error/humanize)
        (log/error))
    (throw (ex-info "Invalid data" {:data data})))
  (log/info "Sending SmartDoc request")
  (let [{:keys [status body]}
        @(http/post (str base-url "/doccheck")
                    {:body (json/write-str data) 
                     :headers (merge base-headers
                                     {"Authorization"
                                      (str "Bearer " (get-token))})})
        _ (when-not (= 200 status)
            (throw (ex-info "Request failed"
                            {:status status :body body})))]
    (log/info "Received SmartDoc response")
    (try
      (json/read-str body :key-fn keyword)
      (catch Exception e
        (spit "err.html" body)
        (throw (ex-info "Failed to parse response"
                        {:status status :body body}))))))

(defn uk-aml-check [data]
  (when-not (m/validate ss-data/uk-aml--schema data)
    (-> ss-data/uk-aml--schema
        (m/explain data)
        (malli.error/humanize)
        (log/error))
    (throw (ex-info "Invalid data" {:data data})))
  (log/info "Sending AML request")
  (let [{:keys [status body]}
        @(http/post (str base-url "/aml")
                    {:body (json/write-str data) 
                     :headers (merge base-headers
                                     {"Authorization"
                                      (str "Bearer " (get-token))})})
        _ (when-not (= 200 status)
            (throw (ex-info "Request failed"
                            {:status status :body body})))]
    (log/info "Received AML response")
    (try
      (json/read-str body :key-fn keyword)
      (catch Exception e
        (spit "err.html" body)
        (throw (ex-info "Failed to parse response"
                        {:status status :body body}))))))

(comment
  ;; Get auth token
  (get-auth)

  (supported-documents)

  (smartdoc-check
    {:client_ref "oliver_test"
     :sanction_region "gbr"
     :name {:title "Mr"
            :first "John"
            :middle "I"
            :last "Smith"}
     :gender "male"
     :date_of_birth "1950-05-26"
     :address {:building "25"
               :street-1 "High Street"
               :town "WESTBURY"
               :region "WILTSHIRE"
               :postcode "BA13 3BN"
               :country "GBR"}
     :issuing_country "GBR"
     :document_type "driving_licence"
     :scan_type "basic_selfie"
     :mobile_number "+447700900090"})


  (let [data {:client_ref "something"
              :risk_level "high"
              :name {:title "Mr" :first "John" :last "Smith"}
              :addresses [{:flat "1" :building "2" :street_1 "3" :street_2 "4"
                           :town "5" :region "6" :postcode "7" :duration 8}
                          {:flat "9" :building "10" :street_1 "11" :street_2 "12"
                           :town "13" :region "14" :postcode "15" :duration 16}]}]
    (uk-aml-check data))

  (uk-aml-check
    {:client_ref "oliver_test"
     ;:cra "experian"
     :risk_level "high"
     :name {:title "Mr"
            :first "John"
            :middle "I"
            :last "Smith"}
     :date_of_birth "1950-05-26"
     ;contacts {:telephone ""
     ;          :mobile "+447700900090"
     ;          :email ""}
     ;:documents {}
     ;:bank {}
     :addresses [{:building "25"
                  :street_1 "High Street"
                  :town "WESTBURY"
                  :region "WILTSHIRE"
                  :postcode "BA13 3BN"
                  :country "GBR"
                  :duration 3}]})

  ;; Test out async requests
  (http/get
    "http://localhost:8080/case/1/identity"
    (fn [resp]
      (println resp))))
