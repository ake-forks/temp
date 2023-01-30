(ns darbylaw.web.ui.decision-tree
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.styles :as styles]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.app-layout :as c]
            [darbylaw.web.ui.landing :as landing]
            [reagent-mui.components :as mui]
            [kee-frame.core :as kf]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; >> Content

(defn question [question]
  [mui/stack {:direction :row
              :align-items :center
              :spacing 1}
   [mui/typography {:variant :body1}
    question]
   [mui/box {:flex-grow 1}]
   [mui/button {:variant :contained
                :sx {:background-color :green}}
    "yes"]
   [mui/button {:variant :contained}
    "no"]])

(defn might-need-probate []
  [mui/container {:max-width :sm
                  :sx {:p 8}}
   [mui/stack {:spacing 4}
    [mui/typography {:variant :h4}
     "you might need probate"]
    [mui/typography {:variant :body1}
     "you can apply for probate, download our free probate guide or request a call back from on of our team to discuss it"]
    [mui/stack {:direction :row
                :spacing 2}
     [mui/button {:variant :contained
                  :on-click #(rf/dispatch [::ui/navigate [:create-case]])}
      "apply for probate"]
     [mui/button {:variant :contained}
      "get our free guide"]
     [mui/button {:variant :contained}
      "request a call back"]]]])

(defn dont-need-probate []
  [mui/container {:max-width :sm
                  :sx {:p 8}}
   [mui/stack {:spacing 4}
    [mui/typography {:variant :h4}
     "it doesn't look like you'll need probate"]
    [mui/typography {:variant :body1}
     "you can download our free probate guide or request a call back from on of our team to discuss it further."]
    [mui/stack {:direction :row
                :spacing 2}
     [mui/button {:variant :contained}
      "get our free guide"]
     [mui/button {:variant :contained}
      "request a call back"]]]])

(defn can-i-apply-for-probate []
  [mui/container {:max-width :sm
                  :sx {:p 8}}
   [mui/stack {:spacing 4}
    [mui/typography {:variant :h4}
     "can I apply for probate?"]
    [mui/typography {:variant :body1}
     "it looks like you could need probate, just a couple more questions and we'll let you know either way."]
    [mui/stack {:spacing 1}
     [mui/divider]
     [question "Did the deceased have a will?"]
     [mui/divider]]]])

(defn do-i-need-probate []
  [mui/container {:max-width :sm
                  :sx {:p 8}}
   [mui/stack {:spacing 4}
    [mui/typography {:variant :h4}
     "do I need probate?"]
    [mui/typography {:variant :body1}
     "please take a minute to answer these questions about the deceased and we'll let you know if you need probate or not."]
    [mui/stack {:spacing 1}
     [mui/divider]
     [question "Did the deceased own a house?"]
     [mui/divider]
     [question "Did they have more than £15,000 in savings?"]
     [mui/divider]]]])

(defn content []
  [:<>
   [do-i-need-probate]
   [can-i-apply-for-probate]
   [dont-need-probate]
   [might-need-probate]])

(defn panel []
  [:<>
   [landing/navbar]
   [content]
   [c/footer]])

(defmethod routes/panels :decision-tree-panel []
  [panel])

