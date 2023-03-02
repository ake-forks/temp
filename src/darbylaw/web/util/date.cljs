(ns darbylaw.web.util.date)

(defn show-local-numeric [date]
  (if (inst? date)
    (.toLocaleString date)
    "-"))

(defn show-date-local-numeric [date]
  (if (inst? date)
    (.toLocaleDateString date)
    "-"))
