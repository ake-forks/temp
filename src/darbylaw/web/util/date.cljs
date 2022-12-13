(ns darbylaw.web.util.date)

(defn show-local-numeric [date]
  (if (inst? date)
    (.toLocaleString date)
    "-"))
