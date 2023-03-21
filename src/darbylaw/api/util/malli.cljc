(ns darbylaw.api.util.malli
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]))

(defn exclusive-keys [keys]
  [:fn {:error/message (str "the following keys are mutually exclusive: "
                         (str/join ", " keys))}
   (fn [m] (not (every? (partial contains? m) keys)))])

(defn when-then [k1 k2]
  [:fn {:error/message (str "missing " k2 ", mandatory as there is " k1)}
   (fn [m] (or (not (contains? m k1))
             (contains? m k2)))])

(defn required [ks]
  [:fn {:error/message (str "the following keys are mandatory: "
                            (str/join ", " ks))}
   (fn [m]
     (every? (partial contains? m) ks))])

(defn if-match
  ([schema if-schema]
   (if-match schema
      if-schema
      any?))
  ([schema if-schema else-schema]
   [:fn {:error/fn (fn [{:keys [value]} _]
                     (if (m/validate schema value)
                       (str "if-schema failed: " (me/humanize (m/explain if-schema value)))
                       (str "else-schema failed: " (me/humanize (m/explain else-schema value)))))}
     (fn [value]
       (if (m/validate schema value)
         (m/validate if-schema value)
         (m/validate else-schema value)))]))
   
(defn when-match
  [schema if-schema]
  (if-match schema
    if-schema))
