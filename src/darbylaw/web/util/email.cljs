(ns darbylaw.web.util.email)

(defn valid-email? [s]
  (re-matches #"[^@\s]+@[^@\s]+\.[^@\s]+" s))

(comment
  (valid-email? "test@example.com")
  (valid-email? "test @example.com")
  (valid-email? "testexample.com"))