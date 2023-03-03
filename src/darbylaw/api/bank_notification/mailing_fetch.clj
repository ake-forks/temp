(ns darbylaw.api.bank-notification.mailing-fetch
  (:require
    [xtdb.api :as xt]))

(defn fetch-letters-to-send [db real|fake]
  (let [send-action (case real|fake
                      :real :send
                      :fake :fake-send)]
    (->> (xt/q db
           '{:find [(pull letter [*])]
             :where [[letter :type :probate.bank-notification-letter]
                     [letter :send-action send-action]
                     (not [letter :upload-state])]
             :in [send-action]}
           send-action)
         (map first))))
