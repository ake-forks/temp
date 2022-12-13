(ns darbylaw.api.util.files
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn create-temp-file [suffix]
  (-> (Files/createTempFile nil suffix (into-array FileAttribute nil))
    .toFile))
