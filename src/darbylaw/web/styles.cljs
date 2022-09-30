(ns darbylaw.web.styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]))

(defcssfn linear-gradient
  ([c1 p1 c2 p2]
   [[c1 p1] [c2 p2]])
  ([dir c1 p1 c2 p2]
   [dir [c1 p1] [c2 p2]]))


(def root-col {:light-grey (rgba 245, 245, 245, 1)
               :mid-grey (rgba 218, 218, 218, 1)
               :green-pantone (rgba 88, 169, 75, 1)
               :jet-grey (rgba 51, 51, 51, 1)
               })

(defglobal defaults
  [:body
   {:color (root-col :jet-grey)
    :font-family "sans-serif"
    :background-color (root-col :light-grey)
    :padding 0
    :margin 0
    :box-sizing :border-box
    :height "100%"

    }]
  [:div]
  [:h1 {:margin 0}]
  [:h2 {:margin 0}]
  [:h3 {:margin 0}]
  [:h4 {:margin 0}])


;(defglobal global-styles
;  {:font-family "'Lexend', sans-serif"
;   :margin "0.5rem"}
;  [:p {:font-family "'Lexend', sans-serif"}]
;  [:h4 {:font-family "'Lexend', sans-serif"}])



(defclass vertical-container []
  {:display :flex
   :flex-direction :column})

(defclass horizontal-container []
  {:display :flex})

(defclass navbar []
  {:display :flex
   :justify-content :space-between
   :padding "1rem"
   })

(defclass timeline []
  {:display :flex
   :flex-direction :column
   :width "100%"
   :height "30vh"

   :background-color (root-col :mid-grey)

   }
  [:h2 {:align-self :flex-start :padding "1rem"}]
  [:.line {
           :margin "1rem"
           :display :flex
           :justify-content :space-evenly
           :height "50%"
           :width "100%"
           :background-color (root-col :jet-grey)}
   [:div {:background-color (root-col :light-grey)
          :flex-grow 1
          :display :flex
          :flex-direction :column
          :justify-content :center
          :align-items :center
          :height "100%"}

    [:h4 {:text-align :center :width "100%" :border-bottom [["1px" "solid" (root-col :jet-grey)]]}]
    ]]
  )





(defclass card []
  {:display :flex
   :flex-direction :column
   :width "20%"
   :background-color (root-col :light-grey)
   :padding "1rem"
   :color (root-col :jet-grey)
   :font "arial"
   :box-shadow [["3px" "3px"]]}
  [:h3 {:color (root-col :green-pantone) :border-bottom [["1px" "solid" (root-col :jet-grey)]]}]
  [:.card-entry
   {:margin-block-start "0.5rem" :background-color (root-col :mid-grey) :padding "2%"}
   [:h4 {:margin-block-start "0.5em"}]
   [:button {:border (root-col :light-grey)}]]
  )



(defclass mui-navbar []
  {:background-color (root-col :light-grey)
   :color (root-col :jet-grey)

   :justify-content :space-between
   }
  [:h5 {:font-weight 600
        :font-family "'Lexend', sans-serif"}]
  [:button {:color (root-col :jet-grey) :text-transform :none :font-weight 600
            :font-family "'Lexend', sans-serif"}])

(defclass mui-dashboard []
  {:padding-top "4rem" :font-family "'Lexend', sans-serif"
   }
  [:h3 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:h4 {:font-weight 400 :font-family "'Lexend', sans-serif"}]
  [:h5 {:font-weight 600 :font-family "'Lexend', sans-serif"}]
  [:button {:background-color (root-col :jet-grey)
            :color (root-col :light-grey)
            :text-transform :none :font-family "'Lexend', sans-serif"
            :border-radius 0}]
  )

