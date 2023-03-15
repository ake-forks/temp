(ns darbylaw.web.ui.components.file-input-button
  (:require [reagent-mui.components :as mui]))

(defn file-input-button [{:keys [button-props
                                 accept
                                 on-selected]}
                         & children]
  [mui/button (merge button-props
                     {:component "label"})
   (into [:<>] children)
   [mui/input {:type :file
               :onChange (fn [e]
                           (let [f (-> e .-target .-files first)]
                             (when on-selected
                               (on-selected f))
                             ; Enables selecting the same file again:
                             (set! (.. e -currentTarget -value) nil)))
               :hidden true
               :sx {:display :none}
               :inputProps {:accept accept}}]])
