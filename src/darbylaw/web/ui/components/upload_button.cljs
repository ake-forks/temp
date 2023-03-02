(ns darbylaw.web.ui.components.upload-button
  (:require [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]))

(defn upload-button [{:keys [filename-atom
                             uploading?-atom
                             on-file-selected
                             input-props
                             button-props]}
                     child]
  [ui/loading-button (merge button-props
                            {:component "label"
                             :loading (when uploading?-atom
                                        (boolean @uploading?-atom))})
   child
   [mui/input {:type :file
               :value (when filename-atom
                        (or @filename-atom ""))
               :onChange #(let [selected-file (-> % .-target .-files first)]
                            (on-file-selected selected-file)
                            (reset! filename-atom "")
                            (reset! uploading?-atom true))
               :hidden true
               :sx {:display :none}
               :inputProps input-props}]])
