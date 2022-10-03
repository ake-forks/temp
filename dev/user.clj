(ns user
  (:require
    [mount.core :as mount]
    [clojure.tools.namespace.repl :as ns-tools]
    [spyscope.core]))

(defn go! []
  (mount/stop)
  (ns-tools/refresh :after 'mount/start))

(comment
  (go!)
  ,)





