(ns darbylaw.api.util.files
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn create-temp-file [prefix suffix]
  (-> (Files/createTempFile prefix suffix (into-array FileAttribute nil))
    .toFile))
