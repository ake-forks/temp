(ns darbylaw.api.bank-notification-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t]
    [darbylaw.api.setup :as setup]
    [darbylaw.api.bank-notification-template :as bank-notification-template]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [darbylaw.api.bank-list :as banks]
    [darbylaw.api.util.data :as data-util]
    [medley.core :as medley]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(defn get-bank-data [case-id]
  (let [resp (t/assert-success (t/run-request (setup/get-case case-id)))
        case (:body resp)]
    (medley/index-by :bank-id (:bank-accounts case))))

(deftest test-bank-notifications
  (let [new-case-resp (t/assert-success (t/run-request (setup/create-case)))
        case-id (-> new-case-resp :body :id)

        {bank-id :id
         bank-name :common-name} (first banks/bank-list)

        _ (t/assert-success
            (t/run-request
              {:request-method :post
               :uri (str "/api/bank/" case-id "/add-bank-accounts")
               :body-params {:bank-id bank-id
                             :accounts setup/accounts1}}))

        ; Check data for rendering letter template
        letter-template-data (bank-notification-template/get-letter-template-data
                               xtdb-node case-id bank-id)
        _ (is (t/submap?
                {:bank {:name bank-name
                        :accounts (-> setup/accounts1
                                    (data-util/keys-to-camel-case))}}
                letter-template-data))

        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request
                 {:request-method :post
                  :uri (str "/api/case/" case-id
                         "/bank/" (name bank-id)
                         "/generate-notification-letter")}))
        _ (is (<= 200 (:status resp) 299))
        bank-data (get (get-bank-data case-id) bank-id)
        _ (is (some? (get-in bank-data [:notification-letter])))]))
