(ns darbylaw.test.common
  (:require [clojure.test :refer :all]
            [mount.core :as mount]
            [darbylaw.config :refer [profile config]]
            [darbylaw.xtdb-node :refer [xtdb-node]]
            [darbylaw.handler :refer [ring-handler]]
            [darbylaw.core :refer [web-server]]
            [darbylaw.api.bank-notification :refer [blank-page]]
            [darbylaw.api.bank-notification-template :refer [templates]]
            [darbylaw.api.death-cert-verif-template :refer [death-certificate-verification-form-template]]
            [xtdb.api :as xt]
            [cognitect.transit :as transit]
            [clojure.string :as str]))

(def test-states
  {#'profile {:start (fn [] :test)}
   #'xtdb-node {:start (fn [] (xt/start-node {}))
                :stop (fn [] (.close xtdb-node))}})

(defn use-mount-states [states]
  (fn [f]
    (mount/stop states)
    (mount/start (-> states
                   (mount/swap-states test-states)))
    (f)
    (mount/stop states)
    (mount/start states)))

(def db-states
  [#'profile #'config #'xtdb-node])

(def ring-handler-states
  (into db-states
    [#'ring-handler
     #'templates #'death-certificate-verification-form-template
     #'blank-page]))

(def web-server-states
  (into ring-handler-states
    [#'web-server]))

(defn make-url [path]
  (str "http://localhost:"
    (-> config :web-server :port)
    path))

(defn submap?
  "Is m1 a submap of m2?"
  [m1 m2]
  (clojure.set/subset? (set m1) (set m2)))

(defn read-transit-body [resp]
  (cond-> resp
    (let [content-type (get-in resp [:headers "Content-Type"])]
      (and (some? content-type)
           (str/includes? content-type "application/transit+json")))
    (update :body #(transit/read (transit/reader % :json)))))

(defn run-request [req]
  (read-transit-body
    (ring-handler
      (cond-> req
        (not (get-in req [:headers "accept"]))
        (update :headers assoc "accept" "application/transit+json")))))

(defn assert-success [resp]
  (assert (<= 200 (:status resp) 299)
    (str "Expected success, but received HTTP status " (:status resp)))
  resp)
