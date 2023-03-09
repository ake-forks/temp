(ns darbylaw.api.smart-search.data)

(defn uk-aml->result
  [{:keys [result]}]
  (keyword result))

(defn fraudcheck->result
  [{:keys [result]}]
  (if (= result "low_risk")
    :pass
    :fail))

(defn smartdoc->status
  [{:keys [status]}]
  (if (contains? #{"processed" "failed" "invalid"} status)
    :processed
    :processing))

(defn smartdoc->result
  [{:keys [result] :as data}]
  (if (= :processed (smartdoc->status data))
    (keyword result)
    :processing))
