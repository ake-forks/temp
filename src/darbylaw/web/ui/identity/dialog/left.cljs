(ns darbylaw.web.ui.identity.dialog.left
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.identity.model :as model]
    [darbylaw.web.util.form :as form]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(defn document-item [case-id {:keys [document-id original-filename]}]
  [mui/stack {:spacing 1
              :direction :row
              :align-items :center}
   [mui/button {:variant :text
                :href (str "/api/case/" case-id "/identity/document/" document-id)
                :target :_blank}
    original-filename]
   [mui/box {:flex-grow 1}]
   [mui/tooltip {:title "Delete"}
    [ui/icon-delete {:style {:cursor :pointer}
                     :on-click #(rf/dispatch [::model/delete-document case-id document-id])}]]])

(defn document-upload-button [case-id]
  [ui/loading-button {:component :label
                      :startIcon (r/as-element
                                   [ui/icon-upload])}
   "upload"
   [:input {:type :file 
            :hidden true
            :on-change #(rf/dispatch [::model/upload-document case-id (-> % .-target .-files first)])}]])

(defn documents []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        documents @(rf/subscribe [::model/user-documents])]
    [mui/stack
     [mui/stack {:spacing 1
                 :direction :row
                 :align-items :center}
      [mui/typography {:variant :h6 
                       :flex-grow 1}
       "documents"]]
     [mui/paper {:sx {:max-height 200 :overflow :scroll}}
      (->> documents
           (map (fn [document]
                  ^{:key (:document-id document)}
                  [document-item case-id document]))
           (interpose [mui/divider])
           (into [mui/stack]))]
     [document-upload-button case-id]]))

(defn note []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        submit-status @(rf/subscribe [::model/note-submit-status])]
    [form/form
     {:on-submit #(rf/dispatch [::model/save-note case-id (:values %)])
      :initial-values {:note @(rf/subscribe [::model/note])}}
     (fn [{:keys [handle-submit] :as fork-args}]
      [mui/stack {:spacing 1}
       [mui/typography {:variant :h6}
        "notes"]
       [form/text-field fork-args
        {:name :note
         :multiline true
         :minRows 5
         :maxRows 5
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
   [documents]])
