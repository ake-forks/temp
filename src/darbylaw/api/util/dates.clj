(ns darbylaw.api.util.dates
  (:import (java.time LocalDateTime)))

(defn instant->localtime [instant zone-id]
  (-> (LocalDateTime/ofInstant instant zone-id)
    (.toLocalTime)))
