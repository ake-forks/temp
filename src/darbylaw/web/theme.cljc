(ns darbylaw.web.theme)

(def html-header-additions
  [[:link {:rel "preconnect"
           :href "https://fonts.googleapis.com"}]
   [:link {:rel "preconnect"
           :href "https://fonts.gstatic.com"
           :crossorigin "true"}]
   [:link {:rel "stylesheet"
           :href "https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap"}]])

(def theme
  {:palette
   {:primary {:main :#067474}
    :secondary {:main :#B08BBF}
    :btn {:main :#121A20
          :light :#121A20
          :dark :#121A20}}

   :typography
   {:font-family "'Poppins', Helvetica,sans-serif"
    :h1 {:font-size "4rem" :font-weight 600}}

   :components
   {:MuiButton {:variants
                [{:props {:variant :contained}
                  :style {:background-color :#121A20
                          :color :#EEEEEE
                          :border-radius 0
                          :text-transform :lowercase}}]}
    :MuiTextField {:defaultProps {:variant :filled}}
    :MuiSelect {:defaultProps {:variant :filled}}}})
