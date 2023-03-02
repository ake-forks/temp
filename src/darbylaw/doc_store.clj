(ns darbylaw.doc-store
  (:require [darbylaw.config :as config]
            [mount.core :as mount]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import (com.amazonaws.services.s3 AmazonS3ClientBuilder AmazonS3)
           (java.io File FileInputStream)
           (com.amazonaws.services.s3.model AmazonS3Exception ListObjectsRequest GetObjectRequest ObjectMetadata)))

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

(defn store
  ([^String key ^File f]
   (when-not (.doesBucketExistV2 s3 bucket-name)
      (.createBucket s3 bucket-name))
   (.putObject s3 bucket-name key f))
  ([^String key f {:keys [content-type]}]
   (when-not (.doesBucketExistV2 s3 bucket-name)
      (.createBucket s3 bucket-name))
   (with-open [fs (io/input-stream f)]
     (let [metadata (doto (ObjectMetadata.)
                      (.setContentType content-type))]
       (.putObject s3 bucket-name key fs metadata)))))

(comment
  ; Dummy implemenatation for dev tests
  (defn store [_key f]
    (println "stored " f)
    (Thread/sleep 1000)))

(defn store-case-file [case-id ^String key ^File f]
  (store (str case-id "/" key) f))

(defn fetch-raw [key]
  (try
    (.getObject s3 ^String bucket-name ^String key)
    (catch AmazonS3Exception exc
      (if (= (.getErrorCode exc) "NoSuchKey")
        (throw (ex-info "Not found" {:error ::not-found} exc))
        (throw exc)))))

(defn fetch [key]
  (-> key fetch-raw .getObjectContent))

(defn fetch-case-file [case-id key]
  (fetch (str case-id "/" key)))

(defn fetch-to-file [key ^File file]
  (try
    (.getObject s3 (GetObjectRequest. bucket-name key) file)
    (catch AmazonS3Exception exc
      (if (= (.getErrorCode exc) "NoSuchKey")
        (throw (ex-info "Not found" {:error ::not-found} exc))
        (throw exc)))))

(defn fetch-case-file-to-file [case-id key file]
  (fetch-to-file (str case-id "/" key) file))

(defn delete-case-file [case-id key]
  (.deleteObject s3 bucket-name (str case-id "/" key)))

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
