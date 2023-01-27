(ns darbylaw.api.util.data
  (:require [camel-snake-kebab.core :as csk]
            [clojure.string :as str])
  (:import (java.time LocalDateTime)))

(defn keys-to-camel-case [m]
  (clojure.walk/postwalk
    #(if (map-entry? %)
       (let [[k v] %]
         (if (namespace k)
           %
           [(csk/->camelCase k :separator #"[.-]") v]))
       %)
    m))

(comment
  (keys-to-camel-case {:to.camel-case :ignore-value :b {:to-camel.case 2} :ns/k :ignore-qualified}))

(defn strip-end [s end]
  (cond-> s
    (str/ends-with? s end)
    (subs 0 (- (count s) (count end)))))

(comment
  (strip-end "hello" "l")
  (strip-end "hello" "lo"))

(defn instant->localtime [instant zone-id]
  (-> (LocalDateTime/ofInstant instant zone-id)
    (.toLocalTime)))
