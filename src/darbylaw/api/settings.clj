(ns darbylaw.api.settings
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.xtdb :as xt-util]
            [reitit.coercion.malli]))

(defn fetch-settings [xtdb-node]
  (xt/entity (xt/db xtdb-node) :probate/settings))

(defn get-settings [{:keys [xtdb-node]}]
  (let [settings (fetch-settings xtdb-node)]
    {:status http/status-200-ok
     :body settings}))

(defn merge-settings [{:keys [xtdb-node body-params]}]
  (xt-util/exec-tx xtdb-node
    (xt-util/deep-merge-tx (assoc body-params
                             :xt/id :probate/settings)))
  {:status http/status-204-no-content})

(defn routes []
  [["/settings" {:get {:handler get-settings}
                 :post {:handler merge-settings
                        :coercion reitit.coercion.malli/coercion
                        :parameters {:body map?}}}]])
