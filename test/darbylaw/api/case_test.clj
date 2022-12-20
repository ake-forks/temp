(ns darbylaw.api.case-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as test-common :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]
    [darbylaw.api.sample-data :as sample]))

(use-fixtures :once
  (test-common/use-mount-states test-common/ring-handler-states))

(defn read-body [resp]
  (cond-> resp
    (:body resp)
    (update :body #(transit/read (transit/reader % :json)))))

(deftest create_and_get_cases
  (let [pr-info sample/pr-info1
        post-resp (read-body
                    (ring-handler
                      {:request-method :post
                       :uri "/api/case"
                       :body-params {:personal-representative pr-info}
                       :headers {"accept" "application/transit+json"}}))]
    (is (<= 200 (:status post-resp) 299))
    (let [{get-status :status
           get-body :body} (ring-handler
                             {:request-method :get
                              :uri "/api/cases"
                              :headers {"accept" "application/transit+json"}})
          cases (transit/read (transit/reader get-body :json))]
      (is (= 200 get-status))
      (is (sequential? cases))
      (is (= 1 (count cases)))
      (let [case (first cases)]
        (is (contains? case :id))
        (is (submap? pr-info (:personal-representative case)))))
    (let [case-id (-> post-resp :body :id)
          updated-data (assoc pr-info
                         :forename "Joe")]
      (let [update-resp (ring-handler
                          {:request-method :put
                           :uri (str "/api/case/"
                                  case-id
                                  "/personal-representative")
                           :body-params updated-data})]
        (is (<= 200 (:status update-resp) 299)))
      (let [{cases :body} (read-body
                            (ring-handler
                              {:request-method :get
                               :uri "/api/cases"
                               :headers {"accept" "application/transit+json"}}))]
        (is (= 1 (count cases)))
        (let [case (first cases)]
          (is (contains? case :id))
          (is (submap? updated-data (:personal-representative case)))))
      (let [{case :body} (read-body
                           (ring-handler
                             {:request-method :get
                              :uri (str "/api/case/" case-id)
                              :headers {"accept" "application/transit+json"}}))]
        (is (submap? updated-data (:personal-representative case)))))))

(comment
  (read-body
    (ring-handler
      {:request-method :get
       :uri "/api/cases"
       :headers {"accept" "application/transit+json"}}))

  (ring-handler
    {:request-method :put
     :uri (str "/api/case/"
            "be757deb-9cda-4424-a1a2-00e7176dc579"
            "/personal-representative")
     :body-params {:street1 "s2",
                   :email "s@s.com",
                   :forename "e",
                   :phone "+441234132412",
                   :town "t",
                   :surname "e",
                   :postcode "pc",
                   :title "t",
                   :date-of-birth "1919-01-01",
                   :street-number "1"}})

  (xtdb.api/entity
    (xtdb.api/db darbylaw.xtdb-node/xtdb-node)
    :darbylaw.api.case/update-ref)
  ,)

(deftest create_case_validation_works
  (let [post-resp (ring-handler
                    {:request-method :post
                     :uri "/api/case"
                     :content-type "application/edn"
                     :body (pr-str {:surname "Doe"
                                    :forename "John"})})]
    (transit/read (transit/reader (:body post-resp) :json))
    (is (= 400 (:status post-resp)))))
