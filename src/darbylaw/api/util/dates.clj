(ns darbylaw.api.util.dates
  (:require
    [clojure.string :as str])
  (:import (java.time LocalDate LocalDateTime)))

(defn instant->localtime [instant zone-id]
  (-> (LocalDateTime/ofInstant instant zone-id)
    (.toLocalTime)))

(defn long-date [local-date with-day?]
  (let [day-name (str/capitalize
                   (str (.getDayOfWeek local-date) " "))
        day-date (.getDayOfMonth local-date)
        ordinal (case (rem day-date 10)
                  1 "st"
                  2 "nd"
                  3 "rd"
                  "th")
        month (str/capitalize (str (.getMonth local-date)))
        year (.getYear local-date)]
    (str (when with-day? day-name) day-date ordinal " " month " " year)))

(defn long-date-from-string [date-string with-day?]
  (let [local-date (LocalDate/parse date-string)]
    (long-date local-date with-day?)))