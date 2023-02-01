(ns darbylaw.web.util.vlad
  (:require [vlad.core :as v]
            [clojure.string :as str]
            [darbylaw.web.util.phone :as phone]
            [darbylaw.web.util.email :as email]
            [darbylaw.web.util.dayjs :as dayjs]))


(defn not-nil
  "Test if a field's value is not nil"
  ([]
   (not-nil {}))
  ([error-data]
   (v/predicate nil? (merge {:type ::not-nil} error-data))))

(defmethod v/english-translation ::not-nil
  [{:keys [name]}]
  (str name " is required."))



(defn valid-dayjs-date
  "Test if a day.js object is a valid date.
  The input value is required to be a day.js object"
  ([]
   (valid-dayjs-date {}))
  ([error-data]
   (v/predicate
     #(not (and (dayjs/date? %)
                (dayjs/valid? %)))
     (merge {:type ::valid-dayjs-date} error-data))))

(defmethod v/english-translation ::valid-dayjs-date
  [{:keys [name]}]
  (str name " is not a valid date."))



(defn valid-phone
  "Test if a string is a valid phone number"
  ([]
   (valid-phone {}))
  ([error-data]
   (v/predicate
     #(not (phone/valid-phone? %))
     (merge {:type ::valid-phone} error-data))))

(defmethod v/english-translation ::valid-phone
  [{:keys [name]}]
  (str name " is not a valid phone."))



(defn valid-email
  "Test if a string is a valid email"
  ([]
   (valid-email {}))
  ([error-data]
   (v/predicate
     #(not (email/valid-email? %))
     (merge {:type ::valid-email} error-data))))

(defmethod v/english-translation ::valid-email
  [{:keys [name]}]
  (str name " is not a valid email."))



(defn present-or-alternative
  "Test if the first selector is present, if not fail and suggest
  the second selector."
  ([selector alt-selector]
   (present-or-alternative selector alt-selector {}))
  ([selector alt-selector error-data]
   (fn [data]
     (when (and (str/blank? (get-in data selector))
                (str/blank? (get-in data alt-selector)))
       [(merge
          {:type ::present-or-alternative
           :selector selector
           :alt-selector alt-selector}
          error-data)]))))

(defmethod v/english-translation ::present-or-alternative
  [{:keys [alt-selector]}]
  (str "...or provide " (do
                          (assert (= 1 (count alt-selector)))
                          ;; TODO: Maybe use `v/assign-name` instead?
                          (case (first alt-selector)
                            :building "Building Name"))))



(defn either-present
  "Test that at least one of the provided selectors is present"
  ([selectors]
   (either-present selectors {}))
  ([selectors error-data]
   (fn [data]
     (when (every? str/blank? (map #(get-in data %) selectors))
       (->> selectors
         (mapv #(merge
                  {:type ::either-present
                   :selector %}
                  error-data)))))))

(defmethod v/english-translation ::either-present
  [{:keys [name]}]
  (str name " is required."))


(defn currency?
  "Checks that a string is a valid currency amount"
  []
  (v/predicate #(nil? (re-matches #"-?[0-9]+(\.[0-9]{2})?" %))
               {:type ::currency?}))

(defmethod v/english-translation ::currency?
  [{:keys [name]}]
  (str name " must be a valid currency amount."))


(defn string-negative?
  "Checks that the string starts with a `-`."
  []
  (v/predicate #(not (str/starts-with? % "-"))
               {:type ::currency-negative?}))

(defmethod v/english-translation ::currency-negative?
  [{:keys [name]}]
  (str name " must be negative."))


(defn v-some?
  ([]
   (v-some? {}))
  ([error-data]
   (v/predicate (complement some?)
     (merge {:type ::v/present} error-data))))
