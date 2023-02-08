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
       [mui/input {:value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::model/upload-file case-id selected-file document-name])
                                (reset! filename "")
                                (reset! model/file-uploading? true))
                   :hidden true
                   :sx {:display :none}
                   :inputProps {:type :file
                                :accept ".pdf, .png, .jpeg, .jpg, .gif"}}]])))

(defn view-button [case-id document-name label]
  [mui/button {:on-click #(rf/dispatch [::model/open-document case-id document-name])
               :variant :contained} label])

(defn get-button [document-name]
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-data @(rf/subscribe [::case-model/current-case])
        label (clojure.string/replace (name document-name) "-" " ")]
    (if (contains? case-data document-name)
      [mui/stack {:spacing 0.5}
       [view-button case-id document-name (str "view " label)]
       [mui/stack {:direction :row
                   :spacing 0.5
                   :justify-content :space-between}
        [mui/typography {:variant :body1}
         (str (get-in case-data [document-name :original-filename]))]
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
     [get-button :death-certificate]
     [get-button :will]
     [get-button :grant-of-probate]]
    [mui/stack {:spacing 1 :sx {:width 0.5}}
     [mui/typography {:variant :body1} "Upload and view key documents related to the case."]
     [ui/???_TO_BE_DEFINED_??? "Currently the document object in the DB records who uploaded the file, when, and the original filename.
        All these fields can be accessed to display in the UI if we want."]
     [ui/???_TO_BE_DEFINED_??? "When files are replaced, the old file can still be retrieved from the DB."]]]])


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
