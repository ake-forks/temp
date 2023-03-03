(ns darbylaw.api.bank-notification.mailing-watchdog
  (:require [xtdb.api :as xt]
            [clojure.set :as set]
            [mount.core :as mount]
            [chime.core :as ch]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.xtdb-node :as xtdb-node]
            [darbylaw.api.bank-notification.mailing-fetch :refer [fetch-letters-to-send]]
            [darbylaw.api.bank-notification.mailing-config :refer [mailing-upload-time]])
  (:import (java.time Period ZonedDateTime ZoneId)
           (java.time.temporal ChronoUnit)))


;; >> Public API

(defn put-with-tx-time [m]
  (tx-fns/invoke ::put-with-tx-data [m]
   '(fn [ctx m]
      (let [tx (xtdb.api/indexing-tx ctx)]
        [[::xt/put (assoc m :timestamp (::xt/tx-time tx))]]))))

(defn watch [letter-id]
  "Create a watch for the letter"
  (let [id {:watchdog.mailing/letter letter-id}]
    (put-with-tx-time (merge {:xt/id id}
                             id))))

(defn unwatch [letter-id]
  "Remove a watch for the letter"
  (let [id {:watchdog.mailing/letter letter-id}]
    [[::xt/delete id]]))



;; >> Clean-up

(defn date->zdt [date]
  "Convert a java.util.Date to a java.time.ZonedDateTime"
  (-> date
      .toInstant
      (.atZone (ZoneId/of "Europe/London"))))

(defn ->is-watch-old? [oldest-allowed-time]
  "Return a predicate that checks if the watch is old enough to be deleted"
  (fn [{:keys [timestamp]}]
    ;; Is `oldest-allowed-time` before `timestamp`
    ;; i.e. is `timestamp` older than `oldest-allowed-time`
    (.isBefore (date->zdt timestamp) oldest-allowed-time)))

(defn clean-watches [xtdb-node watches]
  "Clean up old watches"
  (let [oldest-allowed-time (.minus (ZonedDateTime/now)
                                    30 ChronoUnit/DAYS)]
    (darbylaw.api.util.xtdb/exec-tx xtdb-node
      (->> watches
           (filter (->is-watch-old? oldest-allowed-time))
           (map :watchdog.mailing/letter)
           (map unwatch)
           (apply concat)))))



;; >> Background Service

(defn get-watches
  "Returns a list of the current watches"
  [db]
  (->> (xt/q db
         '{:find [(pull watch [*])]
           :where [[watch :watchdog.mailing/letter _]]})
       (map first)))

(defn check-and-clean [xtdb-node]
  "Check that no letters are being sent twice, and clean up old watches"
  (let [db (xt/db xtdb-node)
        watches (into #{} (get-watches db))
        watches-letter-ids (->> watches (map :watchdog.mailing/letter) (into #{}))

        letters (concat (fetch-letters-to-send db :real)
                        (fetch-letters-to-send db :fake))
        letter-ids (->> letters (map :xt/id) (into #{}))]
    (if-not (empty? (set/intersection letter-ids watches-letter-ids))
      (throw (ex-info "Letters potentially being sent twice" {}))
      (clean-watches xtdb-node watches))))

;; Runs every day one hour after the mailing-job runs
;; The idea is that:
;; - We keep track of letters that have been sent
;; - At any point if we check for to be sent and see if they appear on this list
;; - If they appear on this list we should error to let us know about it
;; Because we don't want these hanging around forever we remove letters from the
;; list after 30 days.
(mount/defstate mailing-watchdog-job
  :start (ch/chime-at
           (ch/periodic-seq
             (-> mailing-upload-time
               ^ZonedDateTime (.adjustInto (ZonedDateTime/now (ZoneId/of "Europe/London")))
               .toInstant
               (.plus 1 ChronoUnit/HOURS))
             (Period/ofDays 1))
           (fn [_time]
             (check-and-clean (xt/db xtdb-node/xtdb-node))))
  :stop (.close mailing-watchdog-job))
