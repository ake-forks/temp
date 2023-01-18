(ns darbylaw.web.ui.banking.validation
  (:require
    [vlad.core :as v]))

(defn get-account-error [errors touched name idx]
  (if (touched idx name)
    (get (nth errors idx) (list name))))

(def bank-account-validation
  (v/join
    (v/attr [:sort-code]
      (v/chain
        (v/present)
        (v/matches #"\d{2}-\d{2}-\d{2}")))
    (v/attr [:account-number]
      (v/chain
        (v/present)
        (v/matches #"[0-9]{8}")))))

(def buildsoc-account-validation
  (v/attr [:roll-number] (v/present)))

(def est-value-validation
  (v/attr [:estimated-value]
    (v/matches #"[0-9]*(\.[0-9]{2})?")))

(def conf-value-validation
  (v/attr [:confirmed-value]
    (v/matches #"[0-9]*(\.[0-9]{2})?")))


(defn add-bank-validation [values]
  (map (fn [acc]
         (merge (v/field-errors bank-account-validation acc)
           (if (not (clojure.string/blank? (:estimated-value acc)))
             (v/field-errors est-value-validation acc))))
    (:accounts values)))



(defn add-buildsoc-validation [values]
  (if (:accounts-unknown values)
    {}
    (map (fn [acc]
           (merge (v/field-errors buildsoc-account-validation acc)
             (if (not (clojure.string/blank? (:estimated-value acc)))
               (v/field-errors est-value-validation acc))))
      (:accounts values))))


(defn value-bank-validation [values]
  (map (fn [acc]
         (merge
           (v/field-errors bank-account-validation acc)
           (if (not (clojure.string/blank? (:confirmed-value acc)))
             (v/field-errors conf-value-validation acc))))
    (:accounts values)))


(defn value-buildsoc-validation [values]
  (map (fn [acc]
         (merge
           (v/field-errors buildsoc-account-validation acc)
           (if (not (clojure.string/blank? (:confirmed-value acc)))
             (v/field-errors conf-value-validation acc))))
    (:accounts values))) 1