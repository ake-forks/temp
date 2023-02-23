(ns darbylaw.api.smart-search.api
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clj-commons.digest :as digest]
            [clojure.tools.logging :as log]
            [darbylaw.api.smart-search.data :as ss-data]
            [darbylaw.api.smart-search.client :refer [apply-middleware base-client]]
            [darbylaw.api.smart-search.auth :refer [wrap-auth]]))


;; >> Authenticated client

(def client
  "An authenticated client"
  (apply-middleware
    base-client
    [wrap-auth]))



;; >> API Calls

(defn lookup-doccheck-supported-documents []
  (client {:method :get
           :path "/lookup/doccheck/supported-documents"
           :body {:country "gbr"}}))

(defn doccheck [data]
  (client {:method :post
           :path "/doccheck"
           :body data}))

(defn get-doccheck [ssid]
  (client {:method :get
           :path (str "/doccheck/" ssid)}))

(defn aml [data]
  (client {:method :post
           :path "/aml"
           :body data}))

(defn fraudcheck [check-type ssid data]
  (when-not (#{"aml" "doccheck"} check-type)
    (log/error "Invalid fraud check check-type")
    (throw (ex-info "Invalid fraud check type" {:check-type check-type})))
  (client {:method :post
           :path (str "/" check-type "/" ssid "/fraudcheck")
           :body data}))

(comment
  (lookup-doccheck-supported-documents)

  (doccheck
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
  (get-doccheck "2912616")

  (aml
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

  (fraudcheck "doccheck" "2912616"
    {:client_ref "oliver_test"
     :sanction_region "gbr"
     :name {:title "Mr"
            :first "John"
            :middle "I"
            :last "Smith"}
     :date_of_birth "1950-05-26"
     :contacts {:mobile "+447700900090"
                :email ""}
     :address {:line_1 (str "25" " " "High Street")
               :city "WESTBURY"
               :postcode "BA13 3BN"
               :country "GBR"}})

  ;; Test out async requests
  (http/get
    "http://localhost:8080/case/1/identity"
    (fn [resp]
      (println resp))))
