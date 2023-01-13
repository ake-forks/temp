(ns darbylaw.api.case-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]
    [darbylaw.api.setup :as sample]
    [clojure.string :as str]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(deftest create_get_update_cases
  ; Create case
  (let [pr-info sample/pr-info1
        post-resp (t/run-request {:request-method :post
                                  :uri "/api/case"
                                  :body-params {:personal-representative pr-info}})]
    (is (<= 200 (:status post-resp) 299))
    ; Get many cases
    (let [{get-status :status
           cases :body} (t/run-request {:request-method :get
                                        :uri "/api/cases"})]
      (is (= 200 get-status))
      (is (sequential? cases))
      (is (= 1 (count cases)))
      (let [case-data (first cases)]
        (is (contains? case-data :id))
        (is (contains? case-data :reference))
        (is (not (:fake case-data)))
        (is (str/ends-with? (:reference case-data) "00"))
        (is (submap? pr-info (:personal-representative case-data)))))

    ; Update personal rep
    (let [case-id (-> post-resp :body :id)
          updated-data (assoc pr-info
                         :forename "Joe")]
      (let [update-resp (t/run-request {:request-method :put
                                        :uri (str "/api/case/"
                                               case-id
                                               "/personal-representative")
                                        :body-params updated-data})]
        (is (<= 200 (:status update-resp) 299)))
      ; Get single case
      (let [{case-data :body} (t/run-request {:request-method :get
                                              :uri (str "/api/case/" case-id)})]
        (is (contains? case-data :id))
        (is (submap? updated-data (:personal-representative case-data))))
      ; Update deceased info
      (let [deceased sample/deceased
            update-resp (t/run-request {:request-method :put
                                        :uri (str "/api/case/"
                                               case-id
                                               "/deceased")
                                        :body-params deceased})
            _ (is (<= 200 (:status update-resp) 299))
            {case :body} (t/assert-success
                           (t/run-request {:request-method :get
                                           :uri (str "/api/case/" case-id)}))
            _ (is (submap? deceased (:deceased case)))
            _ (is (submap? updated-data (:personal-representative case)))]))))

(deftest create-fake-case
  (let [post-resp (t/run-request {:request-method :post
                                  :uri "/api/case"
                                  :body-params {:fake true
                                                :personal-representative sample/pr-info1}})
        _ (is (<= 200 (:status post-resp) 299))
        case-id (-> post-resp :body :id)
        {case-data :body} (t/run-request {:request-method :get
                                          :uri (str "/api/case/" case-id)})
        _ (is (:fake case-data))
        _ (is (str/ends-with? (:reference case-data) "99"))]))

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
