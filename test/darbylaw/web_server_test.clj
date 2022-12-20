(ns darbylaw.web-server-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.test.common :as test-common]
    [org.httpkit.client :as http]))

(use-fixtures :once
  (test-common/use-mount-states test-common/web-server-states))

(deftest healthcheck
  (let [resp @(http/request {:method :get
                             :url (test-common/make-url "/healthcheck")})]
    (is (= 200 (:status resp)))))
