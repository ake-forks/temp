(ns darbylaw.config
  (:require
    [mount.core :as mount]
    [aero.core :as aero]
    [clojure.java.io :as java-io]))

(mount/defstate profile
  :start :production)

(mount/defstate config
  :start (aero/read-config (java-io/resource "config.edn")
           {:profile profile}))
