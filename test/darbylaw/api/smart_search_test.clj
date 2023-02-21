(ns darbylaw.api.smart-search-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [cognitect.transit :as transit]
    [darbylaw.api.setup :as sample]
    [clojure.string :as str]
    [clojure.data.json :as json]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(deftest create_case_validates_against_identity_checks
  ; Create case
  (let [pr-info sample/pr-info1
        post-resp (t/run-request {:request-method :post
                                  :uri "/api/case"
                                  :body-params {:personal-representative pr-info}})
        case-id (-> post-resp :body :id)]
    (is (<= 200 (:status post-resp) 299))

    ; Perform identity check
    (let [check-resp (with-redefs [org.httpkit.client/request
                                   (fn [{:keys [url]} & _]
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
                                       (throw (Exception. "Unexpected URL"))))]
                       (t/run-request {:request-method :post
                                       :uri (str "/api/case/"
                                                 case-id
                                                 "/identity")}))]
      (is (= 200 (:status check-resp))))

    ; Get case and check it has the identity check
    (let [{case-data :body} (t/run-request {:request-method :get
                                            :uri (str "/api/case/" case-id)})
          {:keys [checks]} case-data]
      (is (contains? case-data :id))
      (is (not (nil? checks)))
      (is (= 3 (count checks)))
      (is (every? #(case (:type %)
                     :uk-aml (and (= "pass" (:result %))
                                  (= "1234" (:ssid %)))
                     :smart-doc (and (= "waiting" (:status %))
                                     (= "1234" (:ssid %)))
                     :fraud-check (and (= "processed" (:status %))
                                       (= "low_risk" (:result %))
                                       (= "1234" (:ssid %))))
                  checks)))

    ; Update personal rep
    (let [updated-data (assoc pr-info
                              ;; Title is required to be <=20 chars in length
                              :title "1234567890123456789012345")]
      (let [update-resp (t/run-request {:request-method :put
                                        :uri (str "/api/case/"
                                               case-id
                                               "/personal-representative")
                                        :body-params updated-data})]
        (is (<= 200 (:status update-resp) 299))))

    ; Perform identity check
    ; TODO: Improve this
    ;       This throwing an error isn't ideal, really it should return a 500
    (try (with-redefs [org.httpkit.client/request
                       (fn [{:keys [url]} & _]
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
                           (throw (Exception. "Unexpected URL"))))]
           (t/run-request {:request-method :post
                           :uri (str "/api/case/"
                                     case-id
                                     "/identity")}))
         (is false "Should not reach here")
         (catch Exception e
           (is (= "Invalid data" (ex-message e)))))))
