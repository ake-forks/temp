(ns darbylaw.api.bank-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as test-common]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]
    [darbylaw.api.sample-data :as sample]
    [medley.core :as medley]))

(use-fixtures :once
  (test-common/use-mount-states test-common/ring-handler-states))

(defn read-body [resp]
  (cond-> resp
    (:body resp)
    (update :body #(transit/read (transit/reader % :json)))))

(defn get-case [case-id]
  (read-body
    (ring-handler
      {:request-method :get
       :uri (str "/api/case/" case-id)
       :headers {"accept" "application/transit+json"}})))

(defn post-bank-accounts [op case-id accounts]
  (ring-handler
    {:request-method :post
     :uri (str "/api/bank/" case-id op)
     :body-params {:bank-id :test-bank-id
                   :accounts accounts}
     :headers {"accept" "application/transit+json"}}))

(deftest add_and_update_bank-accounts
  (let [new-case-resp (read-body
                        (ring-handler
                          {:request-method :post
                           :uri "/api/case"
                           :body-params {:personal-representative sample/pr-info1}
                           :headers {"accept" "application/transit+json"}}))
        _ (is (<= 200 (:status new-case-resp) 299))
        case-id (-> new-case-resp :body :id)
        added-accounts [{:sort-code "sort-code1"
                         :account-number "account-number1"}
                        {:sort-code "sort-code2"
                         :account-number "account-number2"}]
        add-resp (post-bank-accounts "/add-bank-accounts" case-id added-accounts)
        _ (is (<= 200 (:status add-resp) 299))
        {case :body} (get-case case-id)
        by-bank-id (medley/index-by :id (:bank-accounts case))
        _ (is (= added-accounts (-> by-bank-id :test-bank-id :accounts)))
        more-added-accounts [{:sort-code "sort-code3"
                              :account-number "account-number3"}
                             {:sort-code "sort-code4"
                              :account-number "account-number4"}]
        add-more-resp (post-bank-accounts "/add-bank-accounts" case-id more-added-accounts)
        _ (is (<= 200 (:status add-more-resp) 299))
        {case :body} (get-case case-id)
        by-bank-id (medley/index-by :id (:bank-accounts case))
        _ (is (= (into added-accounts more-added-accounts)
                (-> by-bank-id :test-bank-id :accounts)))
        new-accounts [{:sort-code "sort-code5"
                       :account-number "account-number5"}
                      {:sort-code "sort-code6"
                       :account-number "account-number6"}]
        add-more-resp (post-bank-accounts "/update-bank-accounts" case-id new-accounts)
        _ (is (<= 200 (:status add-more-resp) 299))
        {case :body} (get-case case-id)
        by-bank-id (medley/index-by :id (:bank-accounts case))
        _ (is (= new-accounts (-> by-bank-id :test-bank-id :accounts)))]))
