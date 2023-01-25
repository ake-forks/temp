(ns darbylaw.web.ui.keydocs.dialog
  (:require
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui :as ui]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.keydocs.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [reagent.core :as r]))

(defn dash-button []
  [mui/button {:on-click #(rf/dispatch [::model/show-dialog])
               :variant :contained
               :size :large
               :style {:background-color theme/teal}} "key documents"])

(defn upload-button [_case-id _file-name _label]
  (r/with-let [_ (reset! model/file-uploading? false)
               filename (r/atom "")]
    (fn [case-id file-name label]
      [ui/loading-button {:component "label"
                          :loading @model/file-uploading?
                          :variant :contained
                          :full-width true
                          :style {:background-color theme/orange}
                          :startIcon (r/as-element [ui/icon-upload])}
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (print (str selected-file))
                                (reset! filename "")
                                (reset! model/file-uploading? true))
                   :hidden true
                   :sx {:display :none}}]])))

(defn view-button [case-id file-name label]
  [mui/button {:on-click #(print file-name)
               :variant :outlined} label])

(defn content []
  (let [case-id @(rf/subscribe [::case-model/case-id])]
    [mui/box {:style {:height "60vh"
                      :padding "1rem"}}
     [mui/stack {:direction :row :spacing 2 :sx {:width 1 :height 1}}
      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [upload-button case-id
        "death-certificate"
        "upload death certificate"]
       [view-button case-id "will" "view will"]
       [view-button case-id "grant-of-probate" "view grant of probate"]]

      [mui/stack {:spacing 1 :sx {:width 0.5}}
       [mui/typography {:variant :body1} "upload and view key documents related to the case"]]]]))


(defn dialog []
  (let [open @(rf/subscribe [::model/dialog])]
    [mui/dialog {:open open :full-width true :max-width :lg}
     [mui/dialog-title
      [mui/stack {:direction :row :justify-content :space-between}
       [mui/typography {:variant :h4} "key documents"]
       [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
        [ui/icon-close]]]]
     [mui/dialog-content
      [content]]
     [mui/dialog-actions
      [mui/button {:on-click #(rf/dispatch [::model/hide-dialog])} "close"]]]))
