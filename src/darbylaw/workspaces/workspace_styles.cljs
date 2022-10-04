(ns darbylaw.workspaces.workspace-styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]
    ))

(def col {:light-grey (rgba 245, 245, 245, 1)               ;#f5f5f5
          :mid-grey (rgba 218, 218, 218, 1)                 ;#dadada
          :green-pantone (rgba 88, 169, 75, 1)              ;#58a94b
          :jet-grey (rgba 51, 51, 51, 1)                    ;#333333
          })

(defclass site-page-header-ghost-wrapper []
  {:background-color (col :mid-grey)
   })

(defclass site-page-header []
  {:border-bottom ("1px" "solid" (rgba 51, 51, 51, 1))})

(defclass mui-default []
  {:padding "0.4rem" :font-family "Lexend, serif" :color (col :jet-grey)}
  [:.MuiCardContent-root {:padding "0.3rem"}]
  [:h1 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h4 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h5 {:margin "0.3rem" :font-size :medium :font-family "Lexend, serif" :color (col :jet-grey)}]
  [:.MuiStepLabel-label {:font-family "'Lexend', sans-serif"}]
  )

(def h {:margin "0.3rem" :font-size :medium :font-family "Lexend, serif" :color (col :jet-grey)})

(defclass icon-buttons []
  [:button {:background-color (col :light-grey)}])