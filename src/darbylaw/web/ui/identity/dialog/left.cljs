(ns darbylaw.web.ui.identity.dialog.left
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.identity.model :as model]
    [darbylaw.web.ui.identity.alert :as alert]
    [darbylaw.web.ui.identity.dialog.utils :refer [check-icon]]
    [darbylaw.web.util.form :as form]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn document-item [{:keys [filename]}]
  [mui/stack {:spacing 1
              :direction :row
              :align-items :center}
   [mui/button {:variant :text
                :on-click #(js/alert "TODO:open file")}
    filename]
   [mui/box {:flex-grow 1}]
   [mui/tooltip {:title "Download"}
    [ui/icon-download {:style {:cursor :pointer}
                       :on-click #(js/alert "TODO: download")}]]
   [mui/tooltip {:title "Replace"}
    [ui/icon-edit {:style {:cursor :pointer}
                   :on-click #(js/alert "TODO: replace")}]]
   [mui/tooltip {:title "Delete"}
    [ui/icon-delete {:style {:cursor :pointer}
                     :on-click #(js/alert "TODO: delete")}]]])

(defn note []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        submit-status @(rf/subscribe [::model/note-submit-status])]
    [form/form
     {:on-submit #(rf/dispatch [::model/save-note case-id (:values %)])
      :initial-values {:note @(rf/subscribe [::model/note])}}
     (fn [{:keys [values handle-submit] :as fork-args}]
      [mui/stack {:spacing 1}
       [mui/typography
        "notes"]
       [form/text-field fork-args
        {:name :note
         :multiline true
         :maxRows 4
         :fullWidth true}]
       [mui/button {:variant :contained
                    :on-click handle-submit
                    :startIcon
                    (when submit-status
                     (r/as-element
                       [ui/icon-check]))}
        "save"]])]))

(defn panel []
  [mui/stack {:spacing 3
              :width "50%"}
   [note]
   [mui/stack
    [mui/stack {:spacing 1
                :direction :row
                :align-items :center}
     [mui/typography {:flex-grow 1}
      "documents"]
     [ui/icon-upload {:style {:cursor :pointer}}]]
    [mui/paper {:sx {:max-height 200 :overflow :scroll}}
     [mui/stack {:spacing 1}
      (for [x (range 9)]
        ^{:key x}
        [document-item {:filename (str x ".pdf")}])]]]])
