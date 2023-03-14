(ns darbylaw.web.ui.landing
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.app-layout :as c]
            [reagent-mui.components :as mui]
            [re-frame.core :as rf]
            [reagent.core :as r]))


;; >> Navbar

(defn app-bar [& body]
  [mui/app-bar {:position :sticky}
   [mui/container {:max-width :xl}
    [mui/toolbar
     (into [:<>] body)]]])

(defn logo []
  [:img {:src "/images/Probate-tree-narrow.png"
         :style {:cursor :pointer}
         :on-click #(rf/dispatch [::ui/navigate [:landing]])}])

(defn menu-item [props & body]
  [mui/menu-item (merge (dissoc props :inner-props))
   [mui/typography (merge {:variant :text
                           :color :black}
                          (:inner-props props))
    (into [:<>] body)]])

(defn navbar []
  [app-bar
   [mui/stack {:direction :row
               :spacing 2}
    [logo]
    [mui/stack {:direction :row}
     [menu-item {:on-click #(rf/dispatch [::ui/navigate [:about]])}
       "about"]
     [menu-item {}
      "prices"]
     [menu-item {}
      "free guide"]]]
   [mui/box {:sx {:flexGrow 1}}]
   [mui/stack {:direction :row
               :spacing 3}
    [mui/button {:start-icon (r/as-element [ui/icon-person-outline])}
     "login"]
    [mui/button {:variant :contained
                 :on-click #(rf/dispatch [::ui/navigate [:admin]])}
     "Admin Panel"]]])



;; >> Probate Made Easy

(defn background [url & body]
  [mui/box
   {:sx {:background-image (str "url(" url ")")
         :background-position "top left"
         :background-size :cover
         :background-repeat :no-repeat}}
   (into [:<>] body)])

(defn tall [& body]
  [mui/box {:sx {:height "80vh"}}
   (into [:<>] body)])

(defn centred [& body]
  [mui/box {:sx {:display :flex
                 :align-items :center
                 :justify-content :center
                 :width :inherit
                 :height :inherit}}
   (into [:<>] body)])

(defn centred-horizontally [& body]
  [mui/box {:sx {:display :flex
                 :justify-content :center
                 :width :inherit
                 :height :inherit}}
   (into [:<>] body)])

(defn centred-vertically [& body]
  [mui/box {:sx {:display :flex
                 :align-items :center
                 :width :inherit
                 :height :inherit}}
   (into [:<>] body)])

;; TODO: Fix so that it fits the screen size permanently
;;       Use javascript?
(def points
  ["lawyer designed case management tools to help you manage the entire probate application at your own pace"
   "automated letters to banks, councils, and utility companies"
   "24/7 access to the progress and next steps of the case via a personalised dashboard"])

(defn probate-made-easy []
  [background "/images/home-trees-1920-1080.jpeg"
   [mui/container {:max-width :xl}
    [mui/stack {}
     [tall
      [centred-vertically
       [mui/grid {:container true}
        [mui/grid {:item true :xs 4}
         [mui/stack {:spacing 3}
          [mui/typography {:variant :h4 :color :white}
            "probate made easy"]
          [mui/typography {:variant :text :color :white}
           "Probate-Tree has been designed by qualified lawyers to guide you through the probate process.
           Our software provides:"]
          (for [line points]
            ^{:key line}
            [mui/list-item {:style {:margin-top 0}}
             [mui/list-item-icon {:style {:color :white}}
              [ui/icon-arrow-forwards]]
             [mui/list-item-text {:style {:color :white}}
              line]])
          [mui/typography {:variant :text :color :white :style {:margin-top 0}}
           "Our lawyer-led team is on-hand to answer your questions and complete the required legal paperwork when the time comes.
           Answer a few questions at the link below to find out if you need probate…"]
          [mui/box {:sx {:pt 2 :mt 0}}
           [mui/button {:variant :contained
                        :size :large
                        :style {:background-color theme/lime
                                :font-weight :normal
                                :textTransform :none}
                        :on-click #(rf/dispatch [::ui/navigate :decision-tree])}
            "do I need probate?"]]]]]]]
     [centred-horizontally
      [mui/stack
       [ui/icon-mouse-outlined {:style {:color :white}}]
       [ui/icon-keyboard-arrow-down {:style {:color :white}}]]]]]])
;; >> Price section

(defn big-price []
  [mui/stack {:direction :row
              :spacing 1
              :align-items :flex-end}
   [mui/typography {:variant :body1
                    :color theme/lime
                    :style {:font-size "1.2rem"}}
    "from"]
   [mui/typography {:variant :h2
                    :color theme/lime}
    "£990"]
   [mui/typography {:variant :body1
                    :color theme/lime
                    :style {:font-size "1.2rem"}}
    "/ probate"]])

(def price-lines
  ["full access to your own case management system"
   "automated notifications to banks and other institutions"
   "Inheritance Tax calculations and form preparation"
   "Probate Court application and final estate accounts"
   "regular updates for beneficiaries and downloadable reports"])

(defn price-text []
 [mui/stack {:spacing 3}
  [mui/typography {:variant :h4
                   :sx {:pb 1}}
   "price"]
  [big-price]
  [mui/typography {:variant :body1
                   :color theme/lime
                   :style {:font-size "1rem"
                           :margin-top 0}}
   "(£825+VAT)"]
  [mui/typography {:variant :text}
   "This fixed fee gives you full access and interaction with your own case, plus guidance and support from our in-house legal team.
   A fee of £273 will also be payable to the Court when we submit your application for probate."]
  [mui/divider]
  [mui/stack {}
   [mui/typography {:variant :text}
    "Our fixed fee includes:"]
   [mui/list
    (for [line price-lines]
      ^{:key line}
      [mui/list-item {:sx {:p 0.5}}
       [mui/list-item-icon
        [ui/icon-check-base]]
       [mui/list-item-text
        line]])]]
  [mui/box
   [mui/button {:variant :contained
                :size :large
                :style {:background-color theme/lime
                        :font-weight :normal
                        :textTransform :none}
                :on-click #(rf/dispatch [::ui/navigate :decision-tree])}
    "do I need probate?"]]])

(defn price-image []
  [mui/box
   {:sx {:background-image (str "url(/images/home-laptop-mock.jpg)")
         :background-position "center center"
         :background-repeat :no-repeat

         ;; Have price image fill available space
         :width "100%"
         :height "100%"}}])

(defn price []
  [mui/container {:max-width :xl
                  :sx {:p 8}}
   [mui/grid {:container true :spacing 8}
    [mui/grid {:item true :xs 4}
     [price-text]]
    [mui/grid {:item true :xs 8
               :justify-content :center
               :align-items :center}
     [price-image]]]])

(defn content []
  [:<>
   [probate-made-easy]
   [price]])

(defn panel []
  [:<>
   [navbar]
   [content]
   [c/footer]])

(defmethod routes/panels :landing-panel []
  [panel])

