(ns darbylaw.api.bank-notification-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t]
    [darbylaw.api.setup :as setup]
    [darbylaw.api.bank-notification :as bank-notification]
    [darbylaw.api.bank-notification-template :as bank-notification-template]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [darbylaw.api.bank-list :as banks]
    [darbylaw.api.buildsoc-list :as buildsocs]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.files :as files-util]
    [medley.core :as medley]
    [clojure.java.io :as io]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(defn get-bank-data [bank-type case-id]
  (let [resp (t/assert-success (t/run-request (setup/get-case case-id)))
        case-data (:body resp)]
    (case bank-type
      :bank (medley/index-by :bank-id (:bank-accounts case-data))
      :buildsoc (medley/index-by :buildsoc-id (:buildsoc-accounts case-data)))))

(defn test-notifications [bank-type]
  (let [new-case-resp (t/assert-success (t/run-request (setup/create-case)))
        case-id (-> new-case-resp :body :id)

        {bank-id :id
         bank-name :common-name}
        (first (case bank-type
                 :bank banks/bank-list
                 :buildsoc buildsocs/buildsoc-list))

        accounts (case bank-type
                   :bank setup/bank-accounts1
                   :buildsoc setup/buildsoc-accounts1)
        _
        (case bank-type
          :bank
          (t/assert-success
            (t/run-request
              {:request-method :post
               :uri (str "/api/bank/" case-id "/add-bank-accounts")
               :body-params {:bank-id bank-id
                             :accounts accounts}}))
          :buildsoc
          (t/assert-success
            (t/run-request
              {:request-method :post
               :uri (str "/api/buildingsociety/" case-id "/add-buildsoc-accounts")
               :body-params {:buildsoc-id bank-id
                             :accounts setup/bank-accounts1}})))

        letter-template-data (bank-notification-template/get-letter-template-data
                               xtdb-node bank-type case-id bank-id)

        ; Check data for rendering letter template
        _ (when (= bank-type :bank)
            (let [_ (is (t/submap?
                          {:bank {:name bank-name
                                  :accounts (-> setup/bank-accounts1
                                              (data-util/keys-to-camel-case))}}
                          letter-template-data))]))

        bank-request (fn [method op & [body]]
                       {:request-method method
                        :uri (str "/api/case/" case-id
                               (case bank-type
                                 :bank "/bank/"
                                 :buildsoc "/buildsoc/")
                               (name bank-id)
                               op)
                        :headers {"accept" "application/transit+json"}
                        :body-params body})

        ; There should be no notification letter yet
        resp (t/run-request (bank-request :get "/notification-pdf"))
        _ (is (= 404 (:status resp)))

        ; Generate letter for the first time
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (bank-request :post "/generate-notification-letter")))
        _ (is (<= 200 (:status resp) 299))
        bank-data (get (get-bank-data bank-type case-id) bank-id)
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
        bank-data (get (get-bank-data bank-type case-id) bank-id)
        _ (is (= "dev" (get-in bank-data [:notification-letter :author])))
        _ (is (= "dev" (get-in bank-data [:notification-letter :by])))

        letter-id (get-in bank-data [:notification-letter :id])

        ; Reviewing the wrong letter
        resp (t/run-request (bank-request :post "/notification-letter/wrong-letter-id/review"
                              {:send-action :send}))
        _ (is (= 404 (:status resp)))

        ; Regenerating the wrong letter
        resp (t/run-request (bank-request :post "/notification-letter/wrong-letter-id/regenerate"))
        _ (is (= 404 (:status resp)))

        ; Regenerating the letter
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (bank-request :post (str "/notification-letter/" letter-id "/regenerate"))))
        _ (is (= 204 (:status resp)))
        bank-data (get (get-bank-data bank-type case-id) bank-id)
        _ (is (= :generated (get-in bank-data [:notification-letter :author])))
        _ (is (= "dev" (get-in bank-data [:notification-letter :by])))

        ; Reviewing the letter
        resp (t/run-request (bank-request :post
                              (str "/notification-letter/" letter-id "/review")
                              {:send-action :send}))
        _ (is (<= 200 (:status resp) 299))
        bank-data (get (get-bank-data bank-type case-id) bank-id)
        _ (is (some? (get-in bank-data [:notification-letter :review-by])))
        _ (is (some? (get-in bank-data [:notification-letter :review-timestamp])))

        ; Regenerating the letter after reviewed is not allowed
        resp (t/run-request (bank-request :post (str "/notification-letter/" letter-id "/regenerate")))
        _ (is (= 409 (:status resp)))

        ; Posting valuation letter
        resp (with-redefs [darbylaw.api.pdf/convert-file (fn [& _])
                           darbylaw.doc-store/store (fn [& _])]
               (t/run-request (merge (bank-request :post "/valuation-pdf")
                                {:multipart-params {"file" {:tempfile (files-util/create-temp-file nil ".pdf")
                                                            :content-type "application/pdf"}
                                                    "filename" "test.pdf"}})))
        _ (is (= 204 (:status resp)))
        bank-data (get (get-bank-data bank-type case-id) bank-id)
        _ (is (some? (get-in bank-data [:valuation-letter])))
        _ (is (= (get-in bank-data [:valuation-letter :uploaded-by]) "dev"))]))

(deftest test-bank-notifications
  (test-notifications :bank))

(deftest test-buildsoc-notifications
  (test-notifications :buildsoc))
