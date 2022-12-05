(ns darbylaw.middleware.xtdb
  (:require
    [darbylaw.xtdb-node :refer [xtdb-node]]))

(defn wrap-xtdb-node [handler]
  (fn [req]
    (handler (-> req
               (assoc :xtdb-node xtdb-node)))))
