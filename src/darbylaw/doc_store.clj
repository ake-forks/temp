(ns darbylaw.doc-store
  (:require [darbylaw.config :as config]
            [mount.core :as mount])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder AmazonS3)))

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

(defn store [key input-stream]
  (when-not (.doesBucketExistV2 s3 bucket-name)
    (.createBucket s3 bucket-name))

  (.putObject s3 bucket-name key input-stream nil))

(defn fetch [key]
  (-> (.getObject s3 ^String bucket-name ^String key)
    (.getObjectContent)))
