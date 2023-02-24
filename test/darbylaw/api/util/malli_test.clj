(ns darbylaw.api.util.malli-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.api.util.malli :as malli+]
    [malli.core :as m]))

(deftest match-then-required
  (testing "single required key"
    (let [schema (malli+/match-then-required
                   [:map
                    [:a [:enum "a" "b"]]]
                   [:b])]
      (is (m/validate schema {:a "a" :b :any}))
      (is (false? (m/validate schema {:a "a"})))
      (is (m/validate schema {:a "c"}))))
  (testing "multiple required keys"
    (let [schema (malli+/match-then-required
                   [:map
                    [:a [:enum "a" "b"]]]
                   [:b :c])]
      (is (m/validate schema {:a "a" :b :any :c :any}))
      (is (false? (m/validate schema {:a "a"})))
      (is (false? (m/validate schema {:a "a" :b :any})))
      (is (false? (m/validate schema {:a "a" :c :any})))
      (is (m/validate schema {:a "c"}))))
  (testing "works with other schemas"
    (let [schema [:and 
                  [:map
                   [:a {:optional true} [:enum "a" "b"]]
                   [:b {:optional true} [:enum "a" "b"]]]
                  (malli+/match-then-required
                    [:map
                     [:a [:enum "a"]]]
                    [:b])]]
      (is (m/validate schema {}))
      (is (m/validate schema {:a "a" :b "a"}))
      (is (false? (m/validate schema {:a "a"})))
      (is (false? (m/validate schema {:a "a" :b :any})))
      (is (m/validate schema {:a "b"}))
      (is (false? (m/validate schema {:a "b" :b :any})))
      (is (m/validate schema {:a "b" :b "a"})))))
