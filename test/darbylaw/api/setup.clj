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

(def accounts1 [{:sort-code "sort-code1"
                 :account-number "account-number1"}
                {:sort-code "sort-code2"
                 :account-number "account-number2"}])

(defn get-case [case-id]
  {:request-method :get
   :uri (str "/api/case/" case-id)})

(defn create-case []
  {:request-method :post
   :uri "/api/case"
   :body-params {:personal-representative pr-info1}})

(defn add-bank-accounts [{:keys [case-id bank-id accounts]
                          :or {bank-id :my-bank-id
                               accounts accounts1}}]
  {:request-method :post
   :uri (str "/api/bank/" case-id "/add-bank-accounts")
   :body-params {:bank-id bank-id
                 :accounts accounts}})