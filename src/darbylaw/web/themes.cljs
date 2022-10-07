(ns darbylaw.web.themes
  (:require
    [reagent-mui.styles :as mui]))

(def theme
  (mui/create-theme {
                     :palette
                     {:primary {:main :#067474}
                      :secondary {:main :#B08BBF}
                      :btn {:main :#121A20
                            :light :#121A20
                            :dark :#121A20}}
                     :typography
                     {:font-family "'Poppins', Helvetica,sans-serif"
                      :h1 {:font-size "4rem" :font-weight 600}}
                     :components
                     {:MuiButton
                      {:variants
                       [{:props {:variant :contained}
                         :style {:background-color :#121A20
                                 :color :#EEEEEE
                                 :border-radius 0
                                 :text-transform :lowercase}}]}
                      :MuiTextField {:defaultProps {:variant :filled}}
                      :MuiSelect {:defaultProps {:variant :filled}}}}))





;Use (apply-theme) on the root MUI element to theme a panel or component
(defn apply-theme [children]
  (mui/theme-provider theme children))






