(ns darbylaw.config
  (:require
    [mount.core :as mount]
    [aero.core :as aero]
    [clojure.java.io :as java-io]
    [clojure.tools.reader.edn :as edn]))

(mount/defstate profile
  :start :production)

;; NOTE: Probably should just call out to secrets manager ourselves
;;       Fine for now while we are just using basic auth though
(defmethod aero/reader 'edn
  [_opts _tag value]
  (edn/read-string value))

(mount/defstate config
  :start (aero/read-config (java-io/resource "config.edn")
           {:profile profile}))
