(ns darbylaw.api.mailing-sync-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is use-fixtures]]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.test.common :as t]
    [darbylaw.api.services.local-sftp-server :as local-sftp]
    [darbylaw.api.services.mailing :as mailing]
    [darbylaw.api.bank-notification.mailing-sync-job :as sync]
    [clojure.java.io :as jio]
    [darbylaw.xtdb-node :refer [xtdb-node]]
    [xtdb.api :as xt]))

(use-fixtures :once
  (t/use-mount-states (into t/db-states
                        [#'local-sftp/ssh-server
                         #'mailing/ssh-session-atom])))

(def local-awaiting-dir (str local-sftp/root-dir "/"))
(def local-errored-dir  (str local-sftp/root-dir "/" sync/errors-dir))

(deftest test_syncing
  (let [letter-ids (vec (for [i (range 6)]
                          (str "letter" i)))
        path0 (str local-errored-dir "/" (get letter-ids 0) ".FailReason0.pdf")
        path1 (str local-errored-dir "/" (get letter-ids 1) ".FailReason1.pdf")
        path2 (str local-errored-dir "/" (get letter-ids 2) ".FailReason2.pdf")
        path3 (str local-awaiting-dir "/" (get letter-ids 3) ".pdf")

        ; Create files in SFTP errors folder
        _ (jio/make-parents path1)
        _ (spit path0 "fake-contents0")
        _ (spit path1 "fake-contents1")
        _ (spit path2 "fake-contents2")
        _ (spit path3 "fake-contents3")

        ; Create some letters in DB
        test-case-id (random-uuid)
        common-letter-data {:type :probate.bank-notification-letter
                            :case-id test-case-id
                            :bank-id :test-bank}
        _ (xt-util/exec-tx xtdb-node
            [[::xt/put (merge common-letter-data
                         {:xt/id (get letter-ids 1)
                          :upload-state :uploaded})]
             [::xt/put (merge common-letter-data
                         {:xt/id (get letter-ids 3)
                          :upload-state :uploaded})]
             [::xt/put (merge common-letter-data
                         {:xt/id (get letter-ids 4)
                          :upload-state :uploaded})]
             [::xt/put (merge common-letter-data
                         {:xt/id (get letter-ids 5)
                          :upload-state :uploaded
                          :send-state :error})]])

        _ (sync/sync! :fake xtdb-node)

        errored-files (->> (file-seq (jio/file local-errored-dir))
                        (map #(.getName %)))

        ; Letters 0 and 2 failed sending, but they are not in the DB, so they must stay.
        _ (is (some #(str/starts-with? % (get letter-ids 0)) errored-files))
        _ (is (some #(str/starts-with? % (get letter-ids 2)) errored-files))

        ; Letter 1 failed sending; it must change state in the DB.
        letter (xt/entity (xt/db xtdb-node) (get letter-ids 1))
        _ (is (= :error (:send-state letter)))
        _ (is (= "FailReason1" (:send-error letter)))
        _ (is (not-any? #(str/starts-with? % (get letter-ids 1)) errored-files))

        ; Letter 3 is awaiting send; state must not change in the DB.
        letter (xt/entity (xt/db xtdb-node) (get letter-ids 3))
        _ (is (nil? (:send-state letter)))

        ; Letter 4 is not found awaiting; it must have been sent.
        letter (xt/entity (xt/db xtdb-node) (get letter-ids 4))
        _ (is (= :sent (:send-state letter)))

        ; Letter 5 has previous send-state; it must not change.
        letter (xt/entity (xt/db xtdb-node) (get letter-ids 5))
        _ (is (= :error (:send-state letter)))]))
