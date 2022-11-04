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
   {:fontFamily "'Poppins', Helvetica,sans-serif"
    :h1 {:fontWeight 700}
    :h2 {:fontWeight 700}
    :h3 {:fontWeight 700}
    :h4 {:fontWeight 700}
    :h5 {:fontWeight 700}
    :button {:fontWeight 600}}

   :components
   {:MuiButton {:variants
                [{:props {:variant :contained}
                  :style {:backgroundColor rich-black
                          :color off-white
                          :borderRadius 0
                          :textTransform :lowercase}}]}
    :MuiTextField {:defaultProps {:variant :filled}}
    :MuiSelect {:defaultProps {:variant :filled}}}})


