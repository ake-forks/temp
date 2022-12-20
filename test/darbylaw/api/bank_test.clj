(ns darbylaw.api.bank-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as test-common]
    [darbylaw.handler :refer [ring-handler]]
    [darbylaw.api.setup :as setup]
    [medley.core :as medley]
    [darbylaw.test.common :as t]))

(use-fixtures :once
  (test-common/use-mount-states test-common/ring-handler-states))

(defn get-accounts-by-bank [case-id]
  (let [{case :body} (t/run-request (setup/get-case case-id))]
    (-> (medley/index-by :id (:bank-accounts case))
      (update-vals :accounts))))

(defn post-bank-accounts [op case-id bank-id accounts]
  (ring-handler
    {:request-method :post
     :uri (str "/api/bank/" case-id op)
     :body-params {:bank-id bank-id
                   :accounts accounts}
     :headers {"accept" "application/transit+json"}}))

(deftest add_and_update_bank-accounts
  (let [new-case-resp (t/run-request (setup/create-case))
        _ (is (<= 200 (:status new-case-resp) 299))
        case-id (-> new-case-resp :body :id)

        added-accounts [{:sort-code "sort-code1"
                         :account-number "account-number1"}
                        {:sort-code "sort-code2"
                         :account-number "account-number2"}]
        add-resp (post-bank-accounts
                   "/add-bank-accounts" case-id :test-bank-id
                   added-accounts)
        _ (is (<= 200 (:status add-resp) 299))
        by-bank-id (get-accounts-by-bank case-id)
        _ (is (= added-accounts (-> by-bank-id :test-bank-id)))

        more-added-accounts [{:sort-code "sort-code3"
                              :account-number "account-number3"}
                             {:sort-code "sort-code4"
                              :account-number "account-number4"}]
        add-more-resp (post-bank-accounts
                        "/add-bank-accounts" case-id :test-bank-id
                        more-added-accounts)
        _ (is (<= 200 (:status add-more-resp) 299))
        by-bank-id (get-accounts-by-bank case-id)
        _ (is (= (into added-accounts more-added-accounts)
                (-> by-bank-id :test-bank-id)))

        new-accounts [{:sort-code "sort-code5"
                       :account-number "account-number5"}
                      {:sort-code "sort-code6"
                       :account-number "account-number6"}]
        new-resp (post-bank-accounts
                   "/update-bank-accounts" case-id :test-bank-id
                   new-accounts)
        _ (is (<= 200 (:status new-resp) 299))
        by-bank-id (get-accounts-by-bank case-id)
        _ (is (= new-accounts (-> by-bank-id :test-bank-id)))

        new-accounts2 [{:sort-code "sort-code7"
                        :account-number "account-number7"}
                       {:sort-code "sort-code8"
                        :account-number "account-number8"}]
        new-resp2 (post-bank-accounts
                    "/update-bank-accounts" case-id :test-bank-id2
                    new-accounts2)
        _ (is (<= 200 (:status new-resp2) 299))
        by-bank-id (get-accounts-by-bank case-id)
        _ (is (= new-accounts (-> by-bank-id :test-bank-id)))
        _ (is (= new-accounts2 (-> by-bank-id :test-bank-id2)))]))

