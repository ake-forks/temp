(ns darbylaw.web.theme)

(def html-header-additions
  [[:link {:rel "preconnect"
           :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect"
           :href "https://fonts.gstatic.com"
           :crossorigin "true"}]
   [:link {:rel "stylesheet"
           :href "https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap"}]])

(def teal "#067474")
(def orange "#E0711C")
(def lilac "#B08BBF")
(def rich-black "#121A20")
(def off-white "#EEEEEE")
(def pale-grey "#DADADA")


(def theme
  {:palette
   {:primary {:main teal}
    :secondary {:main orange}}


   :typography
   {:font-family "'Poppins', Helvetica,sans-serif"

    :h1 {:font-size "3.5rem" :font-weight 600}
    :h2 {:font-size "3rem" :font-weight 600}
    :h3 {:font-size "2.5rem"}}

   :components
   {:MuiButton {:variants
                [{:props {:variant :contained}
                  :style {:background-color rich-black
                          :color off-white
                          :border-radius 0
                          :text-transform :lowercase}}]}
    :MuiTextField {:defaultProps {:variant :filled}}
    :MuiSelect {:defaultProps {:variant :filled}}
    :MuiDivider {:defaultProps {:variant "middle" :role "presentation"}}}})
