(ns build
  (:require [shadow.cljs.devtools.api :as shadow]))

(defn staging []
  (doseq [f [(future (shadow/release :app))
             (future (shadow/compile :cards))]]
    @f))
