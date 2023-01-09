(ns darbylaw.config
  (:require
    [mount.core :as mount]
    [aero.core :as aero]
    [clojure.java.io :as java-io]
    [clojure.tools.reader.edn :as edn]
    [clojure.string :as str]))

(some-> nil
        str/lower-case
        keyword)

(mount/defstate profile
  :start (or (some-> (System/getenv "PROFILE")
                     str/lower-case
                     keyword)
             :test))



;; NOTE: Probably should just call out to secrets manager ourselves
;;       Fine for now while we are just using basic auth though
(defmethod aero/reader 'edn
  [_opts _tag value]
  (edn/read-string value))

(mount/defstate config
  :start (aero/read-config (java-io/resource "config.edn")
           {:profile profile}))
