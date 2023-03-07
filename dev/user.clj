(ns user
  (:require
    [mount.core :as mount]
    [clojure.tools.namespace.repl :as ns-tools]
    [clojure.java.classpath :refer [classpath-directories]]
    [darbylaw.config :refer [profile]]
    [spyscope.core]
    [darbylaw.api.pdf :as pdf]))

;; NOTE: This is a hack to get around the facts that:
;;       - ns-tools/refresh will refresh all files on the classpath by default
;;       - Because we've added kee-frame as a git dependency it will be on the classpath
;;       - kee-frame has a circular dependency in `kee-frame.fsm.beta`
(apply ns-tools/set-refresh-dirs
       (->> (classpath-directories)
            (remove #(re-matches #".*kee-frame.*" (.getAbsolutePath %)))))

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
  (mount/stop (mount/only [#'darbylaw.xtdb-node/xtdb-node]))
  (mount/start)
  ,)
