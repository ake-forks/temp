(ns darbylaw.api.util.malli-test
  (:require
    [clojure.test :refer :all]
    [darbylaw.api.util.malli :as malli+]
    [malli.core :as m]))

(deftest required
  (testing "standard use case"
    (let [schema (malli+/required [:a :b])]
      (is (m/validate schema {:a :any :b :any}))
      (is (false? (m/validate schema {:a :any})))
      (is (false? (m/validate schema {:b :any})))
      (is (m/validate schema {:a :any :b :any :c :any}))))
  (testing "works with other schemas"
    (let [schema [:and
                  [:map
                   [:a {:optional true} [:enum "a" "b"]]]
                  (malli+/required [:a :b])]]
      (is (m/validate schema {:a "a" :b :any}))
      (is (false? (m/validate schema {:a :any :b :any})))
      (is (false? (m/validate schema {:a "a"})))
      (is (false? (m/validate schema {:b :any})))
      (is (m/validate schema {:a "a" :b :any :c :any})))))

(deftest if-match
  (testing "single branch arm"
    (let [schema (malli+/when-match [:map [:a [:enum "a" "b"]]]
                   (malli+/required [:b]))]
      (is (m/validate schema {:a "a" :b :any}))
      (is (false? (m/validate schema {:a "a"})))
      (is (m/validate schema {:a "c"}))))
  (testing "multiple branch arms"
    (let [schema (malli+/if-match [:map [:a [:enum "a" "b"]]]
                   (malli+/required [:b])
                   (malli+/required [:c]))]
      (testing "if branch"
        (is (m/validate schema {:a "a" :b :any}))
        (is (false? (m/validate schema {:a "a"})))
        (is (false? (m/validate schema {:a "a" :c :any}))))
      (testing "else branch"
        (is (m/validate schema {:a "c" :c :any}))
        (is (false? (m/validate schema {:a "c"})))
        (is (false? (m/validate schema {:a "c" :b :any}))))))
  (testing "works with other schemas"
    (let [schema [:and 
                  [:map
                   [:a {:optional true} [:enum "b" "c"]]
                   [:b {:optional true} [:enum "a" "b"]]
                   [:c {:optional true} [:enum "a" "b"]]]
                  (malli+/when-match [:map [:a any?]]
                    (malli+/if-match [:map [:a [:enum "b"]]]
                      (malli+/required [:b])
                      (malli+/required [:c])))]]
      (testing "empty case"
        (is (m/validate schema {})))
      (testing "if branch"
        (is (m/validate schema {:a "b" :b "a"}))
        (is (m/validate schema {:a "b" :b "a" :c "a"}))
        (is (false? (m/validate schema {:a "b"})))
        (is (false? (m/validate schema {:a "b" :b :any}))))
      (testing "else branch"
        (is (m/validate schema {:a "c" :c "a"}))
        (is (m/validate schema {:a "c" :b "a" :c "a"}))
        (is (false? (m/validate schema {:a "c"})))
        (is (false? (m/validate schema {:a "c" :c :any})))))))
