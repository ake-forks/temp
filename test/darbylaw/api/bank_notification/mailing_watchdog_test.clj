(ns darbylaw.api.bank-notification.mailing-watchdog-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.test.common :as t]
    [darbylaw.api.bank-notification.mailing-job :as mailing-job]
    [darbylaw.api.bank-notification.mailing-watchdog :as mailing-watchdog]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [xtdb.api :as xt]))

(use-fixtures :once
  (t/use-mount-states (into t/db-states)))

(deftest test_syncing
  (let [letter-ids (vec (for [i (range 8)]
                          (str "letter" i)))

        ; Create some letters in DB
        test-case-id (random-uuid)
        common-letter-data {:probate.notification-letter/case test-case-id
                            :mail/send-action :send}

        ;; Create some letters sent in the past (more than a month)
        ;; These will be cleaned up later
        _ (xt/await-tx xtdb-node
            (xt/submit-tx xtdb-node
              (concat
                [[::xt/put (merge common-letter-data
                             {:xt/id (get letter-ids 1)
                              :upload-state :uploaded})]]
                (mailing-watchdog/watch (get letter-ids 1))
                [[::xt/put (merge common-letter-data
                             {:xt/id (get letter-ids 2)
                              :upload-state :uploaded})]]
                (mailing-watchdog/watch (get letter-ids 2)))
              {::xt/tx-time #inst "2020"}))

        ; Create some letters sent now
        _ (xt-util/exec-tx xtdb-node
            (concat
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 3)
                            :upload-state :uploaded})]]
              (mailing-watchdog/watch (get letter-ids 3))
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 4)
                            :upload-state :uploaded})]]
              (mailing-watchdog/watch (get letter-ids 4))
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 5)
                            :upload-state :uploaded})]]
              (mailing-watchdog/watch (get letter-ids 5))))

        ; Create some letters to be sent
        _ (xt-util/exec-tx xtdb-node
            (concat
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 6)
                            :send-action :send})]]
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 7)
                            :send-action :send})]]))

        ;; As expected, there are 5 letters being watched
        _ (is (= 5 (count (mailing-watchdog/get-watches (xt/db xtdb-node)))))

        ;; Get letters to send
        letters-to-send (mailing-job/fetch-letters-to-send xtdb-node :real)
        _ (is (= 2 (count letters-to-send)))

        ;; Check there are no duplicates
        _ (is (mailing-watchdog/assert-no-duplicates xtdb-node
                                                     (map :xt/id letters-to-send)))

        ;; Old watches are cleaned up
        _ (is (= 3 (count (mailing-watchdog/get-watches (xt/db xtdb-node)))))

        ;; Mark a letter to be sent again
        _ (xt-util/exec-tx xtdb-node
            (concat
              [[::xt/put (merge common-letter-data
                           {:xt/id (get letter-ids 3)
                            :mail/send-action :send})]]))

        ;; Get letters to send
        letters-to-send (mailing-job/fetch-letters-to-send xtdb-node :real)
        _ (is (= 3 (count letters-to-send)))

        ;; An exception is thrown as letters should not be sent twice
        _ (is (thrown? AssertionError 
                       (mailing-watchdog/assert-no-duplicates xtdb-node
                                                              (map :xt/id letters-to-send))))]))
