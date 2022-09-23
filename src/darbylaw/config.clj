(ns darbylaw.config
  (:require
    [mount.core :as mount]
    [aero.core :as aero]
    [clojure.java.io :as java-io]))

(mount/defstate config
  :start (doto (aero/read-config (java-io/resource "config.edn"))))
