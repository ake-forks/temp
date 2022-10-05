(ns darbylaw.api.case-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as test-common :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]))

(use-fixtures :once
  test-common/use-ring-handler)

(defn add-body-as-str [resp]
  (cond-> resp
    (:body resp) (assoc :body-str (slurp (:body resp)))))

(deftest create_and_get_cases
  (let [pr-info {:surname "Doe"
                 :forename "John"
                 :postcode "SW1W 0NY"}
        post-resp (ring-handler
                    {:request-method :post
                     :uri "/api/case"
                     :body-params {:personal-representative pr-info}})]
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
        (is (submap? pr-info (:personal-representative case)))))))

(deftest create_case_validation_works
  (let [post-resp (ring-handler
                    {:request-method :post
                     :uri "/api/case"
                     :content-type "application/edn"
                     :body (pr-str {:surname "Doe"
                                    :forename "John"})})]
    (transit/read (transit/reader (:body post-resp) :json))
    (is (= 400 (:status post-resp)))))



