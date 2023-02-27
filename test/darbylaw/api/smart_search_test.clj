(ns darbylaw.api.smart-search-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]
    [darbylaw.api.setup :as sample]
    [clojure.string :as str]
    [clojure.data.json :as json]))

(defn fake-handler [{:keys [url]} & _]
  (cond
    (str/ends-with? url "/auth/token")
    (future
      {:status 200
       :body (json/write-str
               {:data {:attributes {:access_token "something"}}})})
    (str/ends-with? url "/aml")
    (future
      {:status 200
       :body (json/write-str
               {:data {:attributes {:ssid "1234" :result "pass"}}})})
    (str/ends-with? url "/doccheck")
    (future
      {:status 200
       :body (json/write-str
               {:data {:attributes {:ssid "1234" :status "waiting"}}})})
    (str/ends-with? url "/fraudcheck")
    (future
      {:status 200
       :body (json/write-str
               {:data {:attributes {:ssid "1234" :status "processed" :result "low_risk"}}})})
    :else
    (throw (Exception. "Unexpected URL"))))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(deftest create_case_validates_against_identity_checks
  ;; Create a case
  (let [pr-info sample/pr-info1
        post-resp (t/run-request {:request-method :post
                                  :uri "/api/case"
                                  :body-params {:personal-representative pr-info}})
        case-id (-> post-resp :body :id)]
    (is (<= 200 (:status post-resp) 299))

    (testing "working case"
      (let [check-resp (with-redefs [org.httpkit.client/request fake-handler]
                        (t/run-request {:request-method :post
                                        :uri (str "/api/case/"
                                                  case-id
                                                  "/identity-checks/run")}))]
        (is (<= 200 (:status check-resp) 299)))

      ;; Get case and check it has the identity check
      (let [{case-data :body} (t/run-request {:request-method :get
                                              :uri (str "/api/case/" case-id)})]
        (is (contains? case-data :id))
        (is (contains? case-data :uk-aml))
        (is (contains? case-data :fraudcheck))
        (is (contains? case-data :smartdoc))
        (let [uk-aml (:uk-aml case-data)]
          (is (and (= "pass" (:result uk-aml))
                   (= "1234" (:ssid uk-aml)))))
        (let [fraudcheck (:fraudcheck case-data)]
          (is (and (= "processed" (:status fraudcheck))
                   (= "low_risk" (:result fraudcheck))
                   (= "1234" (:ssid fraudcheck)))))
        (let [smartdoc (:smartdoc case-data)]
          (is (and (= "waiting" (:status smartdoc))
                   (= "1234" (:ssid smartdoc))))))

      (testing "Override"

        (testing "Set as :pass"
          ;; Override
          (let [override-resp 
                (t/run-request {:request-method :post
                                :uri (str "/api/case/"
                                          case-id
                                          "/identity-checks/override")
                                :query-string (t/->query-string
                                                {:new-result "pass"})})]
            (is (<= 200 (:status override-resp) 299)))
          ;; Check case
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (= :pass (:override-identity-check case-data)))))

        (testing "Reset"
          ;; Override
          (let [override-resp 
                (t/run-request {:request-method :post
                                :uri (str "/api/case/"
                                          case-id
                                          "/identity-checks/override")})]
            (is (<= 200 (:status override-resp) 299)))
          ;; Check case
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (nil? (:override-identity-check case-data))))))

      (testing "SmartSearch returns an error"
        (let [;; Perform checks
              check-resp 
              (with-redefs [org.httpkit.client/request
                            (fn [{:keys [url] :as request} & _]
                              (cond
                                (str/ends-with? url "/aml")
                                (future
                                  {:status 400
                                   :body (json/write-str
                                           {:errors [{:status "400" :title "Bad Request"}]})})
                                :else
                                (fake-handler request)))]
                (t/run-request {:request-method :post
                                :uri (str "/api/case/"
                                          case-id
                                          "/identity-checks/run")}))]
          (is (<= 500 (:status check-resp) 599)))))))
