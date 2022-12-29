(ns darbylaw.api.bank-notification.letter-store)

(defn s3-key [case-id _bank-id filename suffix]
  (str case-id "/" filename suffix))
