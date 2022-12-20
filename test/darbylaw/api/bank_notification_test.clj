(ns darbylaw.api.bank-notification-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t]
    [darbylaw.api.setup :as setup]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(deftest test-bank-notifications
  (let [new-case-resp (t/assert-success (t/run-request (setup/create-case)))
        case-id (-> new-case-resp :body :id)

        _ (t/assert-success
            (t/run-request
              {:request-method :post
               :uri (str "/api/bank/" case-id "/add-bank-accounts")
               :body-params {:bank-id :test-bank-id
                             :accounts setup/accounts1}}))

        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request
                 {:request-method :post
                  :uri (str "/api/case/" case-id
                         "/bank/" (name :test-bank-id)
                         "/start-notification")}))
        _ (is (<= 200 (:status resp) 299))
        case (t/run-request (setup/get-case case-id))
        _ (is (= :started (get-in case [:body :bank :test-bank-id :notification-status])))
        _ (is (= :started (get-in case [:body :bank-notification :test-bank-id :notification-status])))]))
