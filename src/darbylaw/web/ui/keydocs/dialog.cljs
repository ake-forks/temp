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

(defn upload-button [_case-id _document-name _label props]
  (r/with-let [_ (reset! model/file-uploading? false)
               filename (r/atom "")]
    (fn [case-id document-name label]
      [ui/loading-button
       (merge props
         {:component "label"
          :loading @model/file-uploading?
          :startIcon (r/as-element [ui/icon-upload])})
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::model/upload-file case-id selected-file document-name])
                                (reset! filename "")
                                (reset! model/file-uploading? true))
                   :hidden true
                   :sx {:display :none}}]])))

(defn view-button [case-id document-name label]
  [mui/button {:on-click #(rf/dispatch [::model/get-file case-id document-name])
               :variant :contained} label])

(defn get-button [document-name]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        key-docs @(rf/subscribe [::model/key-documents])
        present? @(rf/subscribe [::model/document-present? (keyword document-name)])
        label (clojure.string/replace document-name "-" " ")]
    (if present?
      [mui/stack {:spacing 0.5}
       [view-button case-id document-name (str "view " label)]
       [mui/stack {:direction :row
                   :spacing 0.5
                   :justify-content :space-between}
        [mui/typography {:variant :body1}
         (str (get key-docs (keyword document-name)) ".pdf")]
        [upload-button case-id document-name
         (str "replace " label)
         {:variant :text :size "small"}]]]
      [upload-button case-id document-name
       (str "upload " label)
       {:variant :contained
        :full-width true
        :style {:background-color theme/orange}}])))

(defn content []
  [mui/box {:style {:height "60vh"
                    :padding "1rem"}}
   [mui/stack {:direction :row :spacing 2 :sx {:width 1 :height 1}}
    [mui/stack {:spacing 2 :sx {:width 0.5}}
     [get-button "death-certificate"]
     [get-button "will"]
     [get-button "grant-of-probate"]]
    [mui/stack {:spacing 1 :sx {:width 0.5}}
     [mui/typography {:variant :body1} "upload and view key documents related to the case"]]]])


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
