(ns user
  (:require
    [mount.core :as mount]
    [clojure.tools.namespace.repl :as ns-tools]))

(defn reset! []
  (mount/stop)
  (ns-tools/refresh :after 'mount/start))

(comment
  (reset!)
  ,)





