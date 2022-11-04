(ns darbylaw.web.util.dayjs
  (:require ["dayjs" :as dayjs]))

(defn read [s]
  (dayjs s))

(defn date? [obj]
  (dayjs/isDayjs obj))

(defn valid? [d]
  (.isValid d))

(comment
  (read "2022-01-13")
  (.format (read "2022-01-13") "YYYY-MM-DD"))
