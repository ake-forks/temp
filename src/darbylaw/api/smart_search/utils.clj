(ns darbylaw.api.smart-search.utils
  (:require [mount.core :as mount]
            [darbylaw.config :refer [config]]))


;; >> Config

(mount/defstate base-dashboard-url
  :start (-> config :smart-search :base-url :dashboard))



;; >> Utils

(defn add-dashboard-link [api-path {:keys [ssid] :as check-data}]
  (assoc check-data :dashboard (str base-dashboard-url api-path ssid)))
