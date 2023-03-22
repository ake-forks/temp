(ns darbylaw.api.vehicle-test
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

      (testing "insert vehicle"
        (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                           (t/run-request {:request-method :post
                                           :uri (str "/api/case/" case-id "/vehicle")
                                           :multipart-params {"registration-number" "AB12 CDE"
                                                              "description" "A car"
                                                              "estimated-value" "123.12"
                                                              "-file-1" {:filename "file1"
                                                                         :tempfile (t/test-temp-file "test 1")
                                                                         :content-type "application/text"}
                                                              "-file-2" {:filename "file2"
                                                                         :tempfile (t/test-temp-file "test 2")
                                                                         :content-type "application/text"}}}))]
          (t/assert-success check-resp))

        ;; Get case and check it has the vehicle
        (let [{case-data :body} (t/run-request {:request-method :get
                                                :uri (str "/api/case/" case-id)})]
          (is (contains? case-data :vehicles))
          (is (= 1 (count (:vehicles case-data))))
          (let [vehicle-data (first (:vehicles case-data))]
            (is (= "AB12 CDE" (:registration-number vehicle-data)))
            (is (= "A car" (:description vehicle-data)))
            (is (= "123.12" (:estimated-value vehicle-data)))
            (is (contains? vehicle-data :documents))
            (is (= 2 (count (:documents vehicle-data)))))))

      (testing "insert and edit second vehicle"
        (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                           (t/run-request {:request-method :post
                                           :uri (str "/api/case/" case-id "/vehicle")
                                           :multipart-params {"registration-number" "CD34 EFG"
                                                              "description" "A better car"
                                                              "estimated-value" "234.56"
                                                              "-file-1" {:filename "file3"
                                                                         :tempfile (t/test-temp-file "test 3")
                                                                         :content-type "application/text"}
                                                              "-file-2" {:filename "file3"
                                                                         :tempfile (t/test-temp-file "test 4")
                                                                         :content-type "application/text"}}}))
              vehicle-id (-> check-resp :body :id)]
          (t/assert-success check-resp)

          ;; Get case and check it has the vehicle
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :vehicles))
            (is (= 2 (count (:vehicles case-data))))
            (let [vehicle-data (second (:vehicles case-data))]
              (is (= vehicle-id (:vehicle-id vehicle-data)))
              (is (= "CD34 EFG" (:registration-number vehicle-data)))
              (is (= "A better car" (:description vehicle-data)))
              (is (= "234.56" (:estimated-value vehicle-data)))
              (is (contains? vehicle-data :documents))
              (is (= 2 (count (:documents vehicle-data))))))

          ;; Update the vehicle
          (let [check-resp (with-redefs [darbylaw.doc-store/store (fn [& _])]
                             (t/run-request {:request-method :post
                                             :uri (str "/api/case/" case-id "/vehicle/" vehicle-id)
                                             :multipart-params {"registration-number" "EF56 GHI"
                                                                "description" "A different car"
                                                                "estimated-value" "45.67"
                                                                "-file-1" {:filename "file4"
                                                                           :tempfile (t/test-temp-file "test 4")
                                                                           :content-type "application/text"}}}))]
            (t/assert-success check-resp))

          ;; Get case and check it has the updated vehicle
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :vehicles))
            (is (= 2 (count (:vehicles case-data))))
            (let [vehicle-data (second (:vehicles case-data))]
              (is (= vehicle-id (:vehicle-id vehicle-data)))
              (is (= "EF56 GHI" (:registration-number vehicle-data)))
              (is (= "A different car" (:description vehicle-data)))
              (is (= "45.67" (:estimated-value vehicle-data)))
              (is (contains? vehicle-data :documents))
              (is (= 3 (count (:documents vehicle-data))))
            
              ;; Delete a document
              (let [document-id (-> vehicle-data :documents first :document-id)
                    delete-resp (t/run-request {:request-method :delete
                                                :uri (str "/api/case/" case-id
                                                          "/vehicle/" vehicle-id
                                                          "/document/" document-id)})]
                (t/assert-success delete-resp))))

          ;; Get case and check it has the documents
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (contains? case-data :vehicles))
            (is (= 2 (count (:vehicles case-data))))
            (let [vehicle-data (second (:vehicles case-data))]
              (is (= vehicle-id (:vehicle-id vehicle-data)))
              (is (contains? vehicle-data :documents))
              (is (= 2 (count (:documents vehicle-data)))))))))))
