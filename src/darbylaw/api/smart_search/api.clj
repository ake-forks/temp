(ns darbylaw.api.smart-search.api
  (:require [clojure.tools.logging :as log]
            [darbylaw.api.smart-search.client :refer [apply-middleware wrap-ensure-success base-client]]
            [darbylaw.api.smart-search.auth :refer [wrap-auth]]))


;; >> Authenticated client

(defn client-for-env
  "An authenticated client"
  [env]
  (apply-middleware
    (base-client env)
    [(partial wrap-auth env)
     wrap-ensure-success]))

;; >> API Calls

(defn export-pdf-base64-request [ssid]
  {:method :get
   :path (str "/export/" ssid "/pdf-base64")})

(defn lookup-doccheck-supported-documents []
  {:method :get
   :path "/lookup/doccheck/supported-documents"
   :body {:country "gbr"}})

(defn doccheck-request [data]
  {:method :post
   :path "/doccheck"
   :body data})

(defn get-doccheck-request [ssid]
  {:method :get
   :path (str "/doccheck/" ssid)})

(defn aml-request [data]
  {:method :post
   :path "/aml"
   :body data})

(defn fraudcheck-request [check-type ssid data]
  (when-not (#{"aml" "doccheck"} check-type)
    (log/error "Invalid fraud check check-type")
    (throw (ex-info "Invalid fraud check type" {:check-type check-type})))
  {:method :post
   :path (str "/" check-type "/" ssid "/fraudcheck")
   :body data})

(comment
  (def client
    (client-for-env :fake))

  (client (lookup-doccheck-supported-documents))

  (def doccheck-response
    (client (doccheck-request
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
               :mobile_number "+447700900090"})))
  (client (get-doccheck-request (get-in doccheck-response [:body :data :id])))
  (-> doccheck-response :body :data :id)
  (get-in doccheck-response [:body :data :links :self])

  (def aml-response
    (client (aml-request
              {:client_ref "oliver_test"
               ;:cra "experian"
               :risk_level "high"
               :name {:title "Mr"
                      :first "Ernesto"
                      :middle ""
                      :last "Garcia"}
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
                            :duration 3}]})))

  (keys (get-in aml-response [:body :data :links]))
  (get-in aml-response [:body :data :links :self])

  (def fraudcheck-response
    (client (fraudcheck-request "doccheck" "2912616"
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
                         :country "GBR"}})))
  (get-in fraudcheck-response [:body :data :links :self]))
