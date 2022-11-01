(ns darbylaw.web.util.bank-util)


(defn valid-account-number? [n]
  (re-matches #"[0-9]{8}" n))

(defn valid-sort-code? [n]
  (re-matches #"([0-9]{2}-){2}[0-9]{2}" n))
