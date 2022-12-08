(ns darbylaw.doc-store
  (:require [darbylaw.config :as config]
            [mount.core :as mount]
            [clojure.tools.logging :as log])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder AmazonS3)
           (java.io File)
           (com.amazonaws.services.s3.model AmazonS3Exception ListObjectsRequest)))

; Usage of AWS Java has been preferred over the `cognitect.aws` library, because:
;
; - cognitect.aws does not support presigned URLs
; See https://github.com/cognitect-labs/aws-api/issues/5
;
; - The credentials provider in the AWS SDK for Java allows
; for more options than cognitect.aws.credentials, like
; honoring the "credential_process" option of a profile.
; (See https://github.com/cognitect-labs/aws-api/issues/73)
(mount/defstate ^AmazonS3 s3
  :start (AmazonS3ClientBuilder/defaultClient))

(mount/defstate ^String bucket-name
  :start (get-in config/config [:doc-store :s3-bucket]))

(defn store [^String key ^File f]
  (when-not (.doesBucketExistV2 s3 bucket-name)
    (.createBucket s3 bucket-name))
  (.putObject s3 bucket-name key f))

(comment
  ; Dummy implemenatation for dev tests
  (defn store [key f]
    (println "stored " f)
    (Thread/sleep 1000)))


(defn fetch [key]
  (try
    (-> (.getObject s3 ^String bucket-name ^String key)
      (.getObjectContent))
    (catch AmazonS3Exception exc
      (if (= (.getErrorCode exc) "NoSuchKey")
        (throw (ex-info "Not found" {:error ::not-found} exc))
        (throw exc)))))

(defn available? []
  (try
    (.listObjects s3 (ListObjectsRequest. bucket-name nil nil nil (int 1)))
    true
    (catch Exception exc
      (log/warn exc "S3 connection check failed!")
      false)))

(comment
  (available?)

  (with-redefs [bucket-name "non-existant-bucket"]
    (available?)))
