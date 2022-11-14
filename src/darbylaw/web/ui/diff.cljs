(ns darbylaw.web.ui.diff
  (:require [lambdaisland.deep-diff2 :as diff]
            [clojure.string :as str]))

(def inserted-style
  {:border-style :solid
   :border-color :green
   :background-color :lightgreen
   :width :fit-content})

(def deleted-style
  {:text-decoration :line-through})

(def modified-style
  {:background-color :lightsalmon
   :width :fit-content})

(declare diff-map)

(defn diff-list [coll]
  (into [:ul {:style {:list-style-type :square}}]
    (for [elem coll]
      (let [modified-elem (and (get elem :-) (get elem :+))
            inserted-elem (when (not modified-elem)
                            (get elem :+))
            deleted-elem (when (not modified-elem)
                           (get elem :-))
            elem* (or modified-elem inserted-elem deleted-elem elem)]
        [:li {:style (cond
                       modified-elem modified-style
                       inserted-elem inserted-style
                       deleted-elem deleted-style)}
         (cond
           (map? elem*) (diff-map elem*)
           (sequential? elem*) (diff-list elem*)
           :else (str elem*))]))))

(defn diff-map [m]
  (into [:ul {:style {:list-style-type :none}}]
    (for [[k v] (seq m)]
      (let [inserted-k (get k :+)
            deleted-k (get k :-)
            modified-v (get v :+)]
        (into [:li {:style (cond
                             inserted-k inserted-style
                             deleted-k deleted-style)}]
          (let [k* [:b (str (or inserted-k deleted-k k))]
                v* (or modified-v v)
                v-style (when modified-v {:style modified-style})]
            (cond
              (map? v*)
              [k* [:div v-style (diff-map v*)]]

              (sequential? v*)
              [k* [:i " <array>"] [:div v-style (diff-list v*)]]

              :else
              [k* " " [:span v-style (cond
                                       (nil? v*) [:i "<none>"]
                                       (str/blank? v*) [:i "<blank>"]
                                       :else (str v*))]])))))))

(defn diff [m1 m2]
  (diff-map
    ; `diff` won't do a good job for nil
    (diff/diff
      (or m1 {})
      (or m2 {}))))
