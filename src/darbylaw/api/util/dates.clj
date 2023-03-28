(ns darbylaw.api.util.dates
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as spec])
  (:import (java.time LocalDateTime)))

(defn instant->localtime [instant zone-id]
  (-> (LocalDateTime/ofInstant instant zone-id)
    (.toLocalTime)))

(defn get-ordinal [day-of-month]
  (if (spec/int-in-range? 10 20 day-of-month)
    "th"
    (case (rem day-of-month 10)
      1 "st"
      2 "nd"
      3 "rd"
      "th")))

(defn long-date [local-date with-day?]
  (let [day-name (str/capitalize
                   (str (.getDayOfWeek local-date) " "))
        day-date (.getDayOfMonth local-date)

        month (str/capitalize (str (.getMonth local-date)))
        year (.getYear local-date)]
    (str (when with-day? day-name) day-date " " month " " year)))