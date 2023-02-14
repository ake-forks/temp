(ns darbylaw.api.util.malli
  (:require [clojure.string :as str]))

(defn exclusive-keys [keys]
  [:fn {:error/message (str "the following keys are mutually exclusive: "
                         (str/join ", " keys))}
   (fn [m] (not (every? (partial contains? m) keys)))])

(defn when-then [k1 k2]
  [:fn {:error/message (str "missing " k2 ", mandatory as there is " k1)}
   (fn [m] (or (not (contains? m k1))
             (contains? m k2)))])
