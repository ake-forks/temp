(ns darbylaw.test.common
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [darbylaw.config :refer [profile]]
            [darbylaw.xtdb-node :refer [xtdb-node]]
            [darbylaw.core :refer [web-server]]
            [xtdb.api :as xt]
            [darbylaw.config :as config]))

(def test-states
  {#'profile {:start (fn [] :test)}
   #'xtdb-node {:start (fn [] (xt/start-node {}))
                :stop (fn [] (.close xtdb-node))}})

(defn use-web-server [f]
  (mount/stop)
  (mount/start-with-states test-states)
  (f)
  (mount/stop)
  (mount/start))

(defn use-ring-handler [f]
  (let [states (mount/except [#'web-server])]
    (mount/stop states)
    (-> states
      (mount/swap-states test-states)
      (mount/start))
    (f)
    (mount/stop states)
    (mount/start states)))

(defn make-url [path]
  (str "http://localhost:"
    (-> config/config :web-server :port)
    path))

(defn submap? [m1 m2]
  (clojure.set/subset? (set m1) (set m2)))