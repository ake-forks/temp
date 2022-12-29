(ns darbylaw.api.funeral.expense-store)

(defn s3-key [case-id file-name]
  (str case-id "/funeral-expense-doc/" file-name))
