(ns darbylaw.middleware.auth
  (:require
    [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
    [darbylaw.config :as config]))

(defn authenticated?
  [username password]
  (let [auth (get-in config/config [:web-server :auth])]
    ;; Auth will never be `nil` here as that's checked
    ;; in `create-auth-middleware`
    (assert auth)
    (and (contains? auth username)
         (= (get auth username) password)
         {:username username})))

(defn create-auth-middleware
  [handler authenticated?]
  (let [auth (get-in config/config [:web-server :auth])]
    (if (= auth :none)
      (fn no-auth-handler [req]
        (-> req
            (assoc :basic-authentication {:username "dev"})
            handler))
      (wrap-basic-authentication handler authenticated?))))

(defn add-user-info
  [handler]
  (fn [req]
    (let [user (:basic-authentication req)]
      (-> req
          ;; Add in `:user` key for convenience
          (assoc :user user)
          handler))))
