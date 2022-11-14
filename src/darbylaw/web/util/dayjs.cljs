(ns darbylaw.web.util.dayjs
  (:require ["dayjs" :as dayjs]))

(defn read [s]
  (dayjs s))

(defn maybe-read [s]
  (some-> s
    read))

(defn date? [obj]
  (dayjs/isDayjs obj))

(defn valid? [d]
  (.isValid d))

(defn format-date-for-store [d]
  (.format d "YYYY-MM-DD"))

(comment
  (read "2022-01-13")
  (.format (read "2022-01-13") "YYYY-MM-DD"))
