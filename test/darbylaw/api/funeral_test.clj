(ns darbylaw.api.funeral-test
  (:require
    [clojure.test :refer :all]
    [clojure.set :as set]
    [darbylaw.handler :refer [ring-handler]]
    [darbylaw.api.setup :as setup]
    [darbylaw.test.common :as t]
    [darbylaw.doc-store :refer [s3 bucket-name]]
    [ring.util.io :refer [string-input-stream]]))

(use-fixtures :once
  (t/use-mount-states t/ring-handler-states))

(defn get-funeral-account [case-id]
  (let [{case :body} (t/run-request (setup/get-case case-id))]
    (:funeral-account case)))

(defn get-funeral-expenses [case-id]
  (let [{case :body} (t/run-request (setup/get-case case-id))]
    (:funeral-expense case)))

(defn ->query-string
  [m]
  (ring.util.codec/form-encode m))

(defn upsert-funeral-account [case-id account]
  (ring-handler
    (merge
      {:request-method :put
       :uri (str "/api/case/" case-id "/funeral/account")
       :query-string (->query-string account)
       :headers {"accept" "application/transit+json"}})))

(defn post-other-expense [case-id expense]
  (t/run-request
    {:request-method :post
     :uri (str "/api/case/" case-id "/funeral/other")
     :query-string (->query-string expense)
     :headers {"accept" "application/transit+json"}}))

(defn update-other-expense [case-id expense-id expense]
  (t/run-request
    {:request-method :put
     :uri (str "/api/case/" case-id "/funeral/other/" expense-id)
     :query-string (->query-string expense)
     :headers {"accept" "application/transit+json"}}))

(deftest add_and_update_funeral-account
  (let [new-case-resp (-> (setup/create-case)
                          t/run-request
                          t/assert-success)
        case-id (-> new-case-resp :body :id)

        added-funeral-account {:title "funeral account"
                               :value "1.23"}
        add-resp (-> case-id
                     (upsert-funeral-account added-funeral-account)
                     t/assert-success)
        acct (get-funeral-account case-id)
        _ (is (= added-funeral-account acct))

        updated-funeral-account {:title "funeral account"
                                 :value "1.23"
                                 :paid true
                                 :paid-by "paid by"}
        add-resp (-> case-id
                     (upsert-funeral-account updated-funeral-account)
                     t/assert-success)
        acct (get-funeral-account case-id)
        _ (is (= updated-funeral-account acct))

        added-expenses [{:title "title 1"
                         :value "1.23"}
                        {:title "title 2"
                         :value "2.34"}]
        expense-ids
        (doall
          (for [expense added-expenses]
            (->> expense
                 (post-other-expense case-id)
                 t/assert-success
                 :body :id)))
        expenses (get-funeral-expenses case-id)
        _ (is (= (into #{} expense-ids)
                 (->> expenses (map :expense-id) (into #{}))))
        _ (is (= (into #{} added-expenses)
                 (->> expenses
                      (map #(dissoc % :expense-id))
                      (into #{}))))

        more-added-expenses [{:title "title 3"
                              :value "3.45"}
                             {:title "title 4"
                              :value "4.67"}]
        more-expense-ids
        (doall
          (for [expense more-added-expenses]
            (->> expense
                 (post-other-expense case-id)
                 t/assert-success
                 :body :id)))
        expenses (get-funeral-expenses case-id)
        _ (is (set/subset? (into #{} more-expense-ids)
                           (->> expenses (map :expense-id) (into #{}))))
        _ (is (set/subset? (into #{} added-expenses)
                           (->> expenses
                                (map #(dissoc % :expense-id))
                                (into #{}))))

        updated-expenses (zipmap
                           expense-ids
                           [{:title "title 5"
                             :value "5.67"}
                            {:title "title 6"
                             :value "6.78"}])
        updated-expense-ids
        (doall
          (for [[expense-id expense] updated-expenses]
            (->> (update-other-expense case-id expense-id expense)
                t/assert-success
                :body :id)))
        expenses (get-funeral-expenses case-id)
        _ (is (set/subset?
                (into #{} updated-expense-ids)
                (->> expenses (map :expense-id) (into #{}))))
        expense-by-id (zipmap (map :expense-id expenses)
                              (map #(dissoc % :expense-id) expenses))
        db-updated-expenses (->> updated-expense-ids
                                 (map #(get expense-by-id %))
                                 (filter some?))
        _ (is (= (->> updated-expenses vals (into #{}))
                 (into #{} db-updated-expenses)))]))
