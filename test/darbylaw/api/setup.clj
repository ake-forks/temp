(ns darbylaw.api.setup
  (:require [clojure.test :refer :all]))

(def pr-info1
  {:title "Mr"
   :forename "John"
   :surname "Doe"
   :date-of-birth "1980-01-21"
   :email "john.doe@test.com"
   :phone "+441234123456"
   :street-number "16"
   :street1 "A Street"
   :town "London"
   :postcode "SW1W 0NY"})

(def deceased
  {:relationship "mother"
   :registration-district "registration-district"
   :certificate-number "certificate-number"
   :entry-number "entry-number"
   :date-of-death "2023-01-04"
   :place-of-death "place-of-death"
   :forename "Jane"
   :surname "Doe"
   :sex "female"
   :maiden-name "Roe"
   :date-of-birth "2010-01-03"
   :place-of-birth "London"
   :occupation "occupation"
   :name-of-informant "name-of-informant"
   :cause-of-death "cause-of-death"
   :name-of-doctor-certifying "name-of-doctor-certifying"
   :name-of-registrar "name-of-registrar"})

(def bank-accounts1 [{:sort-code "sort-code1"
                      :account-number "account-number1"}
                     {:sort-code "sort-code2"
                      :account-number "account-number2"}])

(def buildsoc-accounts1 [{:roll-number "roll-number1"
                          :confirmed-value "confirmed-value1"}
                         {:roll-number "roll-number2"
                          :confirmed-value "confirmed-value2"}])

(defn get-case [case-id]
  {:request-method :get
   :uri (str "/api/case/" case-id)})

(defn create-case []
  {:request-method :post
   :uri "/api/case"
   :body-params {:personal-representative pr-info1}})

(defn add-bank-accounts [{:keys [case-id bank-id accounts]
                          :or {bank-id :my-bank-id
                               accounts bank-accounts1}}]
  {:request-method :post
   :uri (str "/api/bank/" case-id "/add-bank-accounts")
   :body-params {:bank-id bank-id
                 :accounts accounts}})