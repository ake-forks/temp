(ns darbylaw.api.bank-notification.letter-store)

(defn s3-key [case-id bank-id suffix]
  (str case-id "/bank-notification/" (name bank-id) suffix))
