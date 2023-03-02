(ns darbylaw.api.util.model
  (:require [xtdb.api :as xt]))

(defn get-reference [xtdb-node case-id]
  (-> (xt/pull (xt/db xtdb-node)
        [:reference] case-id)
    :reference))
