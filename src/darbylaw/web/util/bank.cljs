(ns darbylaw.web.util.bank)

(defn get-id-error [errors name]
  (get (first errors) (list name)))

(defn get-account-error [errors touched name idx]
  (if (touched idx name)
    (get (nth errors (+ idx 1)) (list name))))