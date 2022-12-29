(ns darbylaw.api.bank-notification-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t]
    [darbylaw.api.setup :as setup]
    [darbylaw.api.bank-notification :as bank-notification]
    [darbylaw.api.bank-notification-template :as bank-notification-template]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [darbylaw.api.bank-list :as banks]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.files :as files-util]
    [medley.core :as medley]
    [clojure.java.io :as io]))

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

        bank-request (fn [method op]
                       {:request-method method
                        :uri (str "/api/case/" case-id "/bank/" (name bank-id) op)})

        ; There should be no notification letter yet
        resp (t/run-request (bank-request :get "/notification-pdf"))
        _ (is (= 404 (:status resp)))

        ; Generate letter for the first time
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (bank-request :post "/generate-notification-letter")))
        _ (is (<= 200 (:status resp) 299))
        bank-data (get (get-bank-data case-id) bank-id)
        _ (is (some? (get-in bank-data [:notification-letter])))
        _ (is (= :generated (get-in bank-data [:notification-letter :author])))
        _ (is (= "dev" (get-in bank-data [:notification-letter :by])))

        ; Notification letter should exist
        resp (with-redefs [darbylaw.doc-store/fetch (fn [& _]
                                                      (io/input-stream (.getBytes "test")))]
               (t/run-request (bank-request :get "/notification-pdf")))
        _ (is (= 200 (:status resp)))
        _ (is (= "test" (slurp (:body resp))))

        ; Generating letter a second time should fail
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (bank-request :post "/generate-notification-letter")))
        _ (is (<= 400 (:status resp) 499))

        ; Replacing the letter
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (merge (bank-request :post "/notification-docx")
                                {:multipart-params {"file" {:tempfile (files-util/create-temp-file nil ".docx")
                                                            :content-type bank-notification/docx-mime-type}}})))
        _ (is (= 204 (:status resp)))
        bank-data (get (get-bank-data case-id) bank-id)
        _ (is (= "dev" (get-in bank-data [:notification-letter :author])))
        _ (is (= "dev" (get-in bank-data [:notification-letter :by])))

        letter-id (get-in bank-data [:notification-letter :id])

        ; Approving the wrong letter
        resp (t/run-request (bank-request :post
                              "/approve-notification-letter/wrong-letter-id"))
        _ (is (= 404 (:status resp)))

        ; Approving the letter
        resp (t/run-request (bank-request :post
                              (str "/approve-notification-letter/" letter-id)))
        _ (is (<= 200 (:status resp) 299))
        bank-data (get (get-bank-data case-id) bank-id)
        _ (is (some? (get-in bank-data [:notification-letter :approved])))

        ; Posting valuation letter
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (merge (bank-request :post "/valuation-pdf")
                                {:multipart-params {"file" {:tempfile (files-util/create-temp-file nil ".pdf")
                                                            :content-type "application/pdf"}
                                                    "filename" "test.pdf"}})))
        _ (is (= 204 (:status resp)))
        bank-data (get (get-bank-data case-id) bank-id)
        _ (is (some? (get-in bank-data [:valuation-letter])))
        _ (is (= (get-in bank-data [:valuation-letter :uploaded-by]) "dev"))]))
