(ns darbylaw.api.smart-search-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as t :refer [submap?]]
    [darbylaw.handler :refer [ring-handler]]
    [darbylaw.api.bank-notification :refer [blank-page]]
    [cognitect.transit :as transit]
    [darbylaw.api.setup :as sample]
    [clojure.string :as str]
    [clojure.data.json :as json]
    [clojure.java.io :as io]))

(def smallest-pdf "JVBERi0xLg10cmFpbGVyPDwvUm9vdDw8L1BhZ2VzPDwvS2lkc1s8PC9NZWRpYUJveFswIDAgMyAzXT4+XT4+Pj4+Pg==")

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
    (str/ends-with? url "/pdf-base64")
    (future
      {:status 200
       :body (json/write-str
               {:data {:attributes {:base64 smallest-pdf}}})})
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
    (t/assert-success post-resp)

    (testing "working case"

      (testing "No PDF to download"
        (let [dl-resp (with-redefs [org.httpkit.client/request fake-handler
                                    darbylaw.doc-store/fetch (fn [& _] (io/input-stream blank-page))]
                       (t/run-request {:request-method :get
                                       :uri (str "/api/case/"
                                                 case-id
                                                 "/identity/checks/download-pdf")}))]
          (is (= 404 (:status dl-resp)))))

      (testing "run checks"
        (let [check-resp (with-redefs [org.httpkit.client/request fake-handler
                                       darbylaw.doc-store/store (fn [& _])]
                          (t/run-request {:request-method :post
                                          :uri (str "/api/case/"
                                                    case-id
                                                    "/identity/checks/run")}))]
          (t/assert-success check-resp))

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
                     (= "1234" (:ssid smartdoc)))))))

      (testing "Override"

        (testing "Set as :pass"
          ;; Override
          (let [override-resp 
                (t/run-request {:request-method :post
                                :uri (str "/api/case/"
                                          case-id
                                          "/identity/checks/override")
                                :query-string (t/->query-string
                                                {:new-result "pass"})})]
            (t/assert-success override-resp))
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
                                          "/identity/checks/override")})]
            (t/assert-success override-resp))
          ;; Check case
          (let [{case-data :body} (t/run-request {:request-method :get
                                                  :uri (str "/api/case/" case-id)})]
            (is (nil? (:override-identity-check case-data))))))

      (testing "PDF to download"
        (let [dl-resp (with-redefs [org.httpkit.client/request fake-handler
                                    darbylaw.doc-store/fetch (fn [& _] (io/input-stream blank-page))]
                       (t/run-request {:request-method :get
                                       :uri (str "/api/case/"
                                                 case-id
                                                 "/identity/checks/download-pdf")}))]
          (t/assert-success dl-resp)))

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
                                (fake-handler request)))
                            darbylaw.doc-store/store (fn [& _])]
                (t/run-request {:request-method :post
                                :uri (str "/api/case/"
                                          case-id
                                          "/identity/checks/run")}))]
          (is (<= 500 (:status check-resp) 599)))))))
