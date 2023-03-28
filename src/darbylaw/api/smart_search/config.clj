(ns darbylaw.api.smart-search.config
  (:require [clojure.string :as str]
            [darbylaw.config :refer [config]]))

(defn get-config [env]
  (get-in config [:smart-search env]))

(comment
  (get-config :real)
  (get-config :fake))

(defn link->env [link]
  (cond
    (nil? link) :real
    (str/starts-with? link (-> (get-config :fake) :api-base-url)) :fake
    (str/starts-with? link (-> (get-config :real) :api-base-url)) :real
    :else :real))

(comment
  (link->env "https://sandbox-api.smartsearchsecure.com/doccheck/2939337")
  (link->env "https://api.smartsearchsecure.com/doccheck/2939337")
  (link->env "https://pepe.smartsearchsecure.com/doccheck/2939337"))
