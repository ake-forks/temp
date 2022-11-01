(ns darbylaw.web.util.phone
  (:import [i18n.phonenumbers PhoneNumberUtil PhoneNumberFormat]))

(def phone-util (PhoneNumberUtil/getInstance))

(defn valid-phone? [phone]
  (try
    ; parse may throw exceptions, for example when providing a string too short.
    (let [phone-number (.parse phone-util phone)]
      (.isValidNumber phone-util phone-number))
    (catch :default _
      false)))

(defn format-for-storing [phone]
  (let [phone-number (.parse phone-util phone)]
    (.format phone-util phone-number PhoneNumberFormat/E164)))

(comment
  (format-for-storing "+44 1211 234322"))
