(ns darbylaw.web.styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [darbylaw.web.theme :as theme]
    [spade.core :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]))

(defcssfn linear-gradient
  ([c1 p1 c2 p2]
   [[c1 p1] [c2 p2]])
  ([dir c1 p1 c2 p2]
   [dir [c1 p1] [c2 p2]]))


(defclass navbar []
  {:background-color :white
   :justify-content :space-between
   :padding-left "3rem"
   :padding-right "3rem"}
  [:h5 {:font-weight 600}]
  [:button {:text-transform "lowercase"
            :font-weight 600
            :font-size "large"}])

(defclass footer []
  {:justify-content :space-between
   :color theme/pale-grey
   :background-color :white
   :padding-left "3rem"
   :padding-right "3rem"}
  [:button {:color theme/pale-grey :text-transform "lowercase"}])

(defclass main-content []
  {:padding-top "4rem"})







