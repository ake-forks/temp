(ns darbylaw.test.common
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [darbylaw.config :refer [profile config]]
            [darbylaw.xtdb-node :refer [xtdb-node]]
            [darbylaw.handler :refer [ring-handler]]
            [darbylaw.core :refer [web-server]]
            [xtdb.api :as xt]))

(def test-states
  {#'profile {:start (fn [] :test)}
   #'xtdb-node {:start (fn [] (xt/start-node {}))
                :stop (fn [] (.close xtdb-node))}})

(defn use-mount-states [states]
  (fn [f]
    (mount/stop (keys test-states))
    (mount/start (-> states
                   (mount/swap-states test-states)))
    (f)
    (mount/stop states)
    (mount/start states)))

(def ring-handler-states
  [#'profile #'config #'xtdb-node #'ring-handler])

(def web-server-states
  (into ring-handler-states
    [#'web-server]))

(defn make-url [path]
  (str "http://localhost:"
    (-> config :web-server :port)
    path))

(defn submap? [m1 m2]
  (clojure.set/subset? (set m1) (set m2)))