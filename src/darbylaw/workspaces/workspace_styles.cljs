(ns darbylaw.workspaces.workspace-styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]
    ))

(def col {:light-grey (rgba 245, 245, 245, 1)
          :mid-grey (rgba 218, 218, 218, 1)
          :green-pantone (rgba 88, 169, 75, 1)
          :jet-grey (rgba 51, 51, 51, 1)
          })

(defclass mui-default []
  {:padding "0.2rem"}
  [:.MuiCardContent-root {:padding "0.3rem"}]
  [:h1 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h4 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h5 {:margin "0.2rem" :font-size :medium :font-family "Lexend, serif" :color (col :jet-grey)}])

(def h {:margin "0.2rem" :font-size :medium :font-family "Lexend, serif" :color (col :jet-grey)})