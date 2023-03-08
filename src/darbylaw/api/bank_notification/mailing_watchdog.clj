(ns darbylaw.api.bank-notification.mailing-watchdog
  (:require [xtdb.api :as xt]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [darbylaw.api.util.tx-fns :as tx-fns])
  (:import (java.time Period ZonedDateTime ZoneId)
           (java.time.temporal ChronoUnit)))


;; >> XTDB Utils

(defn put-with-tx-time [m]
  (tx-fns/invoke ::put-with-tx-data [m]
   '(fn [ctx m]
      (let [tx (xtdb.api/indexing-tx ctx)]
        [[::xt/put (assoc m :timestamp (::xt/tx-time tx))]]))))



;; DB API

(defn watch [letter-id]
  "Create a watch for the letter"
  (let [id {:watchdog.mailing/letter letter-id}]
    (put-with-tx-time (merge {:xt/id id}
                             id))))

(defn unwatch [letter-id]
  "Remove a watch for the letter"
  (let [id {:watchdog.mailing/letter letter-id}]
    [[::xt/delete id]]))

(defn get-watches
  "Returns a list of the current watches"
  [db]
  (->> (xt/q db
         '{:find [(pull watch [*])]
           :where [[watch :watchdog.mailing/letter _]]})
       (map first)))



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



;; >> Assert functions

(defn assert-no-duplicates [xtdb-node letter-ids]
  "Assert no letters are being sent twice, if not clean up old watches"
  (let [watches (into #{} (get-watches (xt/db xtdb-node)))
        watched-letter-ids (->> watches (map :watchdog.mailing/letter) (into #{}))
        letter-ids (into #{} letter-ids)
        duplicate-letter-ids (set/intersection letter-ids watched-letter-ids)]
    (when-not (empty? duplicate-letter-ids)
      (log/errorf "Found %s duplicate letters: %s"
                 (count duplicate-letter-ids) 
                 (str/join ", " duplicate-letter-ids))
      (throw (AssertionError. "Letters potentially being sent twice")))
    (clean-watches xtdb-node watches)))
