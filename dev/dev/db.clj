(ns dev.db
  (:require [darbylaw.xtdb-node :refer [xtdb-node]]
            [xtdb.api :as xt]
            [mount.core :as mount]))

(comment
  (mount/start)
  (.close xtdb-node)
  (xt/status xtdb-node)

  (xt/q
    (xt/db xtdb-node)
    '{:find [(pull doc [*])]
      :where [[doc :type type]]
      :in [typep]}
    :probate.case))