(ns user
  (:require
    [mount.core :as mount]
    [clojure.tools.namespace.repl :as ns-tools]
    [darbylaw.config :refer [profile]]
    [spyscope.core]
    [darbylaw.api.pdf :as pdf]))

(def dev-states
  {#'profile {:start (fn [] :dev)}})

(defn start-dev! []
  (mount/start-with-states dev-states))

(defn go! []
  (mount/stop (mount/except [#'pdf/office-manager]))
  (ns-tools/refresh :after 'user/start-dev!))

(comment
  (go!)

  (mount/stop (mount/only [#'pdf/office-manager]))
  (mount/start)
  ,)
