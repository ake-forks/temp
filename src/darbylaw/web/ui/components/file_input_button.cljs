(ns darbylaw.web.ui.components.file-input-button
  (:require [reagent-mui.components :as mui]))

(defn file-input-button [{:keys [button-props
                                 accept
                                 on-file-selected
                                 on-load-data-url]}
                         & children]
  [mui/button (merge button-props
                     {:component "label"})
   (into [:<>] children)
   [mui/input {:type :file
               :onChange (fn [e]
                           (let [f (-> e .-target .-files first)]
                             (when on-file-selected
                               (on-file-selected f))
                             (when on-load-data-url
                               (let [r (js/FileReader.)]
                                 (set! (. r -onload)
                                   #(on-load-data-url (.. % -target -result)))
                                 (. r readAsDataURL f)))))
               :hidden true
               :sx {:display :none}
               :inputProps {:accept accept}}]])
