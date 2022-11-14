(ns darbylaw.workspaces.workspace-styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]))


(def col {:light-grey (rgba 245, 245, 245, 1)               ;#f5f5f5
          :mid-grey (rgba 218, 218, 218, 1)                 ;#dadada
          :green-pantone (rgba 88, 169, 75, 1)              ;#58a94b
          :jet-grey (rgba 51, 51, 51, 1)})                  ;#333333


;-----------------------------------------------------------------------
;AntD

(defclass antd-dashboard []
  {:padding-top "2rem" :font-family "'Lexend', sans-serif"}

  [:h1 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h3 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h4 {:font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:h5 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h6 {:font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:span {:font-family "'Lexend', sans-serif"}]
  [:button {:margin "3px"}])



(defclass site-page-header-ghost-wrapper []
  {:background-color (col :mid-grey)})


(defclass site-page-header []
  {:border-bottom ("1px" "solid" (rgba 51, 51, 51, 1))})


;-----------------------------------------------------------------------
;MUI

(defclass mui-navbar []
  {:background-color (col :light-grey)
   :color (col :jet-grey)

   :justify-content :space-between}

  [:h5 {:font-weight 600
        :font-family "'Lexend', sans-serif"}]
  [:button {:color (col :jet-grey) :text-transform :none :font-weight 600
            :font-family "'Lexend', sans-serif"}])

(defclass mui-default []
  {:padding "0.4rem" :font-family "Lexend, serif" :color (col :jet-grey)}
  [:.MuiCardContent-root {:padding "0.3rem"}]
  [:h1 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h4 {:font-family "Lexend, serif" :color (col :jet-grey)}]
  [:h5 {:margin "0.3rem" :font-size :medium :font-family "Lexend, serif" :color (col :jet-grey)}]
  [:.MuiStepLabel-label {:font-family "'Lexend', sans-serif"}])


(defclass mui-dashboard []
  {:font-family "'Lexend', sans-serif"}
  [:h3 {:font-size :x-large :font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h4 {:font-size :large :font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:h5 {:font-size :x-large :font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h6 {:font-size :x-large :font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:span {:font-family "'Lexend', sans-serif"}]

  [:button {
            :color (col :light-grey)
            :background-color (col :jet-grey)
            :text-transform :none :font-family "'Lexend', sans-serif"
            :borderRadius 0}

   [:&:hover {:background-color :#525252 :border "1px solid #525252"}]]


  [:.MuiSlider-colorPrimary {:color (col :green-pantone)}]
  [:.MuiCheckbox-colorSecondary {:color (col :green-pantone)}]
  [:.MuiSwitch-colorPrimary {:color (col :green-pantone)}]

  [:.MuiPaper-root {:padding "1rem"}
   [:h5 {:padding "1rem"}]
   [:span {:padding "1rem"}]])


(defclass mui-bank []
  {:font-family "'Lexend', sans-serif"}
  [:h3 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h4 {:font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:h6 {:font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:.MuiFilledInput-root:before {:font-family "'Lexend', sans-serif" :border-bottom 0}]
  [:.MuiButton-text {:font-size "large"
                     :font-weight 600
                     :font-family "'Lexend', sans-serif"
                     :text-transform :none
                     :justify-content "flex-start"
                     :color (col :jet-grey)}]
  [:.MuiButton-contained {
                          :color (col :light-grey)
                          :background-color (col :jet-grey)
                          :text-transform :none :font-family "'Lexend', sans-serif"
                          :borderRadius 0}

   [:&:hover {:background-color :#525252}]])