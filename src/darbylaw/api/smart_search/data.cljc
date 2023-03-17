(ns darbylaw.api.smart-search.data
  (:require [clojure.string :as str]))

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

(defn build-dashboard-base-url [link]
  (cond
    (nil? link)
    "https://smartsearchsecure.com"

    (str/starts-with? link "https://sandbox-api.smartsearchsecure.com")
    "https://sandbox.smartsearchsecure.com"

    (str/starts-with? link "https://api.smartsearchsecure.com")
    "https://smartsearchsecure.com"

    :else
    "https://smartsearchsecure.com"))

(defn aml-dashboard-link [aml-data]
  (str (build-dashboard-base-url (:links-self aml-data))
       "/aml/results/"
       (:ssid aml-data)))

(defn fraudcheck-dashboard-link [fraudcheck-data]
  (str (build-dashboard-base-url (:links-self fraudcheck-data))
       "/aml/results/"
       (:ssid fraudcheck-data)))

(defn smartdoc-dashboard-link [smartdoc-data]
  (str (build-dashboard-base-url (:links-self smartdoc-data))
       "/doccheck/results/"
       (:ssid smartdoc-data)))
