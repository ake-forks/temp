(ns darbylaw.api.other-test
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [darbylaw.test.common :as t]
    [darbylaw.api.setup :as sample]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(deftest can-add-vehicles
  ;; Create a case
  (let [pr-info sample/pr-info1
        post-resp (t/run-request {:request-method :post
                                  :uri "/api/case"
                                  :body-params {:personal-representative pr-info}})
        case-id (-> post-resp :body :id)]
    (t/assert-success post-resp)

    (testing "working case"

      (testing "insert other asset"
        (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                           (t/run-request {:request-method :post
                                           :uri (str "/api/case/" case-id "/other")
                                           :multipart-params {"name" "Necklace"
                                                              "note" "very pretty"
                                                              "value" "123.12"
                                                              "-file-1" {:filename "file1"
                                                                         :tempfile (t/test-temp-file "test 1")
                                                                         :content-type "application/text"}
                                                              "-file-2" {:filename "file2"
                                                                         :tempfile (t/test-temp-file "test 2")
                                                                         :content-type "application/text"}}}))]
          (t/assert-success check-resp))

        ;; Get case and check it has the asset
        (let [{case-data :body} (t/run-request {:request-method :get
                                                :uri (str "/api/case/" case-id)})]
          (is (contains? case-data :other-assets))
          (is (= 1 (count (:other-assets case-data))))
          (let [asset-data (first (:other-assets case-data))]
            (is (= "Necklace" (:name asset-data)))
            (is (= "very pretty" (:note asset-data)))
            (is (= "123.12" (:value asset-data)))
            (is (contains? asset-data :documents))
            (is (= 2 (count (:documents asset-data)))))))

      (testing "insert and edit second asset"
        (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                           (t/run-request {:request-method :post
                                           :uri (str "/api/case/" case-id "/other")
                                           :multipart-params {"name" "Ring"
                                                              "note" "Hidden in the fireplace"
                                                              "value" "234.56"
                                                              "-file-1" {:filename "file3"
                                                                         :tempfile (t/test-temp-file "test 3")
                                                                         :content-type "application/text"}
                                                              "-file-2" {:filename "file3"
                                                                         :tempfile (t/test-temp-file "test 4")
                                                                         :content-type "application/text"}}}))
              asset-id (-> check-resp :body :id)]
          (t/assert-success check-resp)

          ;; Get case and check it has the asset
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :other-assets))
            (is (= 2 (count (:other-assets case-data))))
            (let [asset-data (second (:other-assets case-data))]
              (is (= asset-id (:asset-id asset-data)))
              (is (= "Ring" (:name asset-data)))
              (is (= "Hidden in the fireplace" (:note asset-data)))
              (is (= "234.56" (:value asset-data)))
              (is (contains? asset-data :documents))
              (is (= 2 (count (:documents asset-data))))))

          ;; Update the asset
          (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                             (t/run-request {:request-method :post
                                             :uri (str "/api/case/" case-id "/other/" asset-id)
                                             :multipart-params {"name" "The one ring"
                                                                "note" "Now in a different kind of fireplace :'("
                                                                "value" "0"
                                                                "-file-1" {:filename "file4"
                                                                           :tempfile (t/test-temp-file "test 4")
                                                                           :content-type "application/text"}}}))]
            (t/assert-success check-resp))

          ;; Get case and check it has the updated asset
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :other-assets))
            (is (= 2 (count (:other-assets case-data))))
            (let [asset-data (second (:other-assets case-data))]
              (is (= asset-id (:asset-id asset-data)))
              (is (= "The one ring" (:name asset-data)))
              (is (= "Now in a different kind of fireplace :'(" (:note asset-data)))
              (is (= "0" (:value asset-data)))
              (is (contains? asset-data :documents))
              (is (= 3 (count (:documents asset-data))))
            
              ;; Delete a document
              (let [document-id (-> asset-data :documents first :document-id)
                    delete-resp (t/run-request {:request-method :delete
                                                :uri (str "/api/case/" case-id
                                                          "/other/" asset-id
                                                          "/document/" document-id)})]
                (t/assert-success delete-resp))))

          ;; Get case and check it has the documents
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :other-assets))
            (is (= 2 (count (:other-assets case-data))))
            (let [asset-data (second (:other-assets case-data))]
              (is (= asset-id (:asset-id asset-data)))
              (is (contains? asset-data :documents))
              (is (= 2 (count (:documents asset-data)))))))))))
