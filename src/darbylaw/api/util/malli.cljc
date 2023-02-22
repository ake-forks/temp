(ns darbylaw.api.util.malli
  (:require [clojure.string :as str]
            [malli.core :as m]))

(defn exclusive-keys [keys]
  [:fn {:error/message (str "the following keys are mutually exclusive: "
                         (str/join ", " keys))}
   (fn [m] (not (every? (partial contains? m) keys)))])

(defn when-then [k1 k2]
  [:fn {:error/message (str "missing " k2 ", mandatory as there is " k1)}
   (fn [m] (or (not (contains? m k1))
             (contains? m k2)))])

(defn match-then-required [schema ks]
  [:fn {:error/message (str "the following keys are mandatory because of a precondition: "
                            (str/join ", " ks))}
   (fn [m]
     (or (not (m/validate schema m))
         (every? (partial contains? m) ks)))])
