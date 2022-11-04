(ns user
  (:require
    [mount.core :as mount]
    [clojure.tools.namespace.repl :as ns-tools]
    [darbylaw.config :refer [profile]]
    [spyscope.core]))

(def dev-states
  {#'profile {:start (fn [] :dev)}})

(defn start-dev! []
  (mount/start-with-states dev-states))

(defn go! []
  (mount/stop)
  (ns-tools/refresh :after 'user/start-dev!))

(comment
  (reset!)
  ,)
