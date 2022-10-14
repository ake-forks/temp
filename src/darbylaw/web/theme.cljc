(ns darbylaw.web.theme)

(def html-header-additions
  [[:link {:rel "preconnect"
           :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect"
           :href "https://fonts.gstatic.com"
           :crossorigin "true"}]
   [:link {:rel "stylesheet"
           :href "https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700&display=swap"}]])

(def teal "#067474")
(def orange "#B08BBF")
(def rich-black "#121A20")
(def off-white "#EEEEEE")

(def theme
  {:palette
   {:primary {:main teal}
    :secondary {:main orange}
    :btn {:main rich-black
          :light rich-black
          :dark rich-black}}

   :typography
   {:font-family "'Poppins', Helvetica,sans-serif"
    :h1 {:font-weight 700}
    :h2 {:font-weight 700}
    :h3 {:font-weight 700}
    :h4 {:font-weight 700}
    :h5 {:font-weight 700}
    :button {:font-weight 600}}

   :components
   {:MuiButton {:variants
                [{:props {:variant :contained}
                  :style {:background-color rich-black
                          :color off-white
                          :border-radius 0
                          :text-transform :lowercase}}]}
    :MuiTextField {:defaultProps {:variant :filled}}
    :MuiSelect {:defaultProps {:variant :filled}}}})
