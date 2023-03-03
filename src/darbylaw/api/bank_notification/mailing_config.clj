(ns darbylaw.api.bank-notification.mailing-config
  (:require [darbylaw.config :as config]
            [mount.core :as mount])
  (:import (java.time LocalTime)))

(mount/defstate mailing-upload-time
  :start (-> config/config :mailing-service :upload-time LocalTime/parse))
