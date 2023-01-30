(ns darbylaw.web.ui.landing
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.styles :as styles]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.app-layout :as c]
            [reagent-mui.components :as mui]
            [kee-frame.core :as kf]
            [re-frame.core :as rf]
            [reagent.core :as r]))


;; >> Navbar

(defn app-bar [& body]
  [mui/app-bar {:position :sticky}
   [mui/container {:max-width :xl}
    [mui/toolbar
     (into [:<>] body)]]])

(defn logo []
  [mui/typography {:variant :h6 
                   :sx {:color theme/rich-black}}
    "probate-tree"])

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
     [menu-item {:on-click #(rf/dispatch [::ui/navigate [:landing]])}
       "about"]
     [menu-item {:on-click #(rf/dispatch [::ui/navigate [:landing]])}
      "prices"]
     [menu-item {:on-click #(rf/dispatch [::ui/navigate [:landing]])}
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
         :background-repeat :repeat}}
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
(defn probate-made-easy []
  [background "/images/grey-squares.png"
   [mui/container {:max-width :xl}
    [mui/stack {}
     [tall
      [centred-vertically
       [mui/grid {:container true}
        [mui/grid {:item true :xs 4}
         [mui/stack {:spacing 3}
          [mui/typography {:variant :h4}
            "probate made easy"]
          [mui/typography {:variant :text}
           "Lorem ipsum dolor sit amet, consetetur sadipscing
elitr, s ed diam nonumy eirmod tempor invidunt ut
labore e t dolore magna aliquyam erat, sed diam volu
ptua. At  vero eos et accusam et justo duo dolores et
ea rebum . Stet clita kasd gubergren, no sea takimata
sanctus  est Lorem."]
          [mui/box {:sx {:pt 2}}
           [mui/button {:variant :contained
                        :on-click #(rf/dispatch [::ui/navigate :create-case])}
            "do I need probate?"]]]]]]]
     [centred-horizontally
      [mui/stack
       [ui/icon-mouse-outlined]
       [ui/icon-keyboard-arrow-down]]]]]])



;; >> Price section

(defn big-price []
  [mui/stack {:direction :row
              :spacing 1
              :align-items :flex-end}
   [mui/typography {:variant :body1
                    :style {:font-size "1.2rem"}}
    "from"]
   [mui/typography {:variant :h2}
    "£750"]
   [mui/typography {:variant :body1
                    :style {:font-size "1.2rem"}}
    "/ probate"]])

(def price-lines
  ["Lorem ipsum dolor sit amet, consetetur."
   "Ipsum dolor sit amet orem."
   "Dolor sit amet consetetu ametr."
   "Lorem ipsum dolor sit amet."
   "Sit amet consetetur lorem ipsum dolor."])

(defn price-text []
 [mui/stack {:spacing 3}
  [mui/typography {:variant :h4
                   :sx {:pb 1}}
   "price"]
  [big-price]
  [mui/typography {:variant :text}
   "Lorem ipsum dolor sit amet, consetetur
sadipscing elitr, sed diam nonumy eirmod
tempor invidunt ut labore et dolore."]
  [mui/divider]
  [mui/stack {}
   [mui/typography {:variant :text}
    "Includes:"]
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
                :on-click #(rf/dispatch [::ui/navigate :create-case])}
    "do i need probate?"]]])

(defn price-image []
  [mui/box
   {:sx {:background-image (str "url(/images/grey-squares.png)")
         :background-position "top left"
         :background-repeat :repeat

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

