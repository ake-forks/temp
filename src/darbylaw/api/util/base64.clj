(ns darbylaw.api.util.base64
  (:import [java.util Base64]))

(defn decode-base64 [data]
  (.decode (Base64/getDecoder) (.getBytes data)))
