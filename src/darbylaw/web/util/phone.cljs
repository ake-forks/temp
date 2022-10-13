(ns darbylaw.web.util.phone
  (:require [applied-science.js-interop :as j])
  (:import [i18n.phonenumbers PhoneNumberUtil PhoneNumberFormat]))

(def phone-util (j/call PhoneNumberUtil :getInstance))

(defn valid-phone? [phone]
  (try
    ; parse may throw exceptions, for example when providing a string too short.
    (let [phone-number (.parse phone-util phone)]
      (j/call phone-util :isValidNumber phone-number))
    (catch :default _
      false)))

(defn format-for-storing [phone]
  (let [phone-number (.parse phone-util phone)]
    (j/call phone-util :format phone-number PhoneNumberFormat/E164)))

(comment
  (format-for-storing "+44 1211 234322"))
