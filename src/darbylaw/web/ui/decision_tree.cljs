(ns darbylaw.web.ui.decision-tree
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.styles :as styles]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.util.form :as form-util]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.app-layout :as c]
            [darbylaw.web.ui.landing :as landing]
            [reagent-mui.components :as mui]
            [kee-frame.core :as kf]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; >> Question Util

(def mui-no-disabled-button-style
  {:color "rgba(0,0,0,0.26)"
   :background-color "rgba(0,0,0,0.12)"
   :box-shadow :none})

(def mui-yes-disabled-button-style
  {:color "#fafdfa"
   :background-color "#b8e5b1"
   :box-shadow :none})

(defn question
  "A form element that presents a question followed by two buttons: yes and no.
  When one is clicked the the text is greyed and the other button is greyed but
  remains clickable."
  [{:keys [values touched set-values]}
   {:keys [name question]}]
  [mui/stack {:spacing 1}
   [mui/stack {:direction :row
               :align-items :center
               :spacing 1}
    [mui/typography {:variant :body1
                     :style
                     (when (touched name)
                       {:color :grey})}
     question]
    [mui/box {:flex-grow 1}]
    [mui/button {:variant :contained
                 :sx {:background-color "#1aac00"}
                 :on-click #(set-values {name true})
                 :style
                 (when (and (touched name) (true? (values name)))
                   mui-yes-disabled-button-style)}
     "yes"]
    [mui/button {:variant :contained
                 :on-click #(set-values {name false})
                 :style
                 (when (and (touched name) (false? (values name)))
                   mui-no-disabled-button-style)}
     "no"]]
   [mui/divider]])



;; >> Layouts

(defn question-list [{:keys [values touched] :as fork-args}]
  [mui/container {:max-width :sm
                  :sx {:p 8}}
   [mui/stack {:spacing 4}
    [mui/typography {:variant :h4}
     "do I need probate?"]
    [mui/typography {:variant :body1}
     "please take a minute to answer these questions about the deceased and we'll let you know if you need probate or not."]
    [mui/stack {:spacing 1}
     [mui/divider]
     [question fork-args
      {:name :know-already
       :question "Do you already know if you need probate?"}]
     [question fork-args
      {:name :own-house
       :question "Did the deceased own a house?"}]
     ;; NOTE: This question doesn't make sense if the deceased didn't own a
     ;;       house so let's hide it
     ;; NOTE: For `mui/collapse` "in" means the content is shown on the page
     [mui/collapse {:in (or (not (touched :own-house))
                            (true? (values :own-house)))}
      [question fork-args
       {:name :sole-owner
        :question "Was it in their sole name?"}]]
     [question fork-args
      {:name :savings
       :question "Did they have more than £15,000 in savings?"}]
     [question fork-args
      {:name :all-join-accounts
       :question "Are all their bank accounts in joint names?"}]
     [question fork-args
      {:name :stocks-shares
       :question "Did they have any Stocks & Shares accounts?"}]]]])

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



;; >> Content

(defn layout [{:keys [handle-submit values touched]
               :as fork-args}]
  [:form {:on-submit handle-submit}
   [question-list fork-args]
   (cond
     (or (true? (values :know-already))
         (and (true? (values :own-house))
             (true? (values :sole-owner)))
         (true? (values :savings))
         (false? (values :all-join-accounts))
         (true? (values :stocks-shares)))
     [might-need-probate fork-args]

     (and (false? (values :savings))
          (true? (values :all-join-accounts))
          (false? (values :stocks-shares)))
     [dont-need-probate fork-args])])

(defonce form-state (r/atom nil))

(defn content []
  [form-util/form
   {:state form-state
    :clean-on-unmount true
    :keywordize-keys true
    :prevent-default? true}
   layout])

(defn panel []
  [:<>
   [landing/navbar]
   [content]
   [c/footer]])

(defmethod routes/panels :decision-tree-panel []
  [panel])
