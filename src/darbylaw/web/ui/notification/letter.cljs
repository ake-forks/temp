(ns darbylaw.web.ui.notification.letter
  (:require [darbylaw.web.ui :as ui]
            [darbylaw.web.util.date :as date-util]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.notification.model :as model]))

(def confirmation-dialog-open? (r/atom false))
(def override-fake-send? (r/atom false))

(defn send-confirmation-dialog []
  [mui/dialog {:open (boolean @confirmation-dialog-open?)
               :maxWidth :sm
               :fullWidth true}
   [mui/dialog-title "confirm send"]
   [mui/dialog-content
    [mui/box {:sx {:mt 2}}
     [mui/typography {:sx {:mb 1}}
      "This is a " [:b "fake"] " case, and therefore no real letter will be posted. "
      "You can override that for testing purposes:"]
     [mui/form-control-label
      {:checked @override-fake-send?
       :onChange (fn [_ev checked] (reset! override-fake-send? checked))
       :control (r/as-element [mui/switch])
       ;:disabled (not= @review-result :send)
       :label "Post a real letter!"}]
     (when @override-fake-send?
       [mui/alert {:severity :warning}
        "Ensure there is a proper test address on the letter!"])]]
   [mui/dialog-actions]])

(def edit-anchor (r/atom nil))

(defn edit-popover []
  (let [close! #(reset! edit-anchor nil)]
    [mui/popover {:open (some? @edit-anchor)
                  :anchorEl @edit-anchor
                  :onClose close!
                  :anchorOrigin {:vertical :bottom
                                 :horizontal :right}
                  :transformOrigin {:vertical :top
                                    :horizontal :right}
                  :PaperProps {:sx {:border-radius 0}}}
     [mui/container {:maxWidth :sm
                     :sx {:my 2}}
      [mui/typography {:variant :body1
                       :font-weight :bold}
       (cond
         ;(= author :unknown-user)
         ;"This notification letter was modified by a user."
         ;
         ;(string? author)
         ;(str "This notification letter was modified by '" author "'.")

         :else
         "This letter was automatically generated from case data.")]
      [ui/loading-button {:onClick #(do (close!)
                                        (rf/dispatch [::TODO] #_[::regenerate asset-type case-id asset-id letter-id]))
                          :loading false ;@regenerating?
                          :startIcon (r/as-element [ui/icon-refresh])
                          :variant :outlined
                          :sx {:mt 1}}
       "Regenerate letter from current case data"]
      [mui/typography {:variant :body1
                       :sx {:mt 2}}
       "You can modify the letter using Word."]
      [mui/typography {:variant :body2}
       "(Be careful in keeping the first page layout intact, "
       "as the address must match the envelope's window)."]
      [mui/stack {:direction :row
                  :spacing 1
                  :sx {:mt 1}}
       [mui/button {;:href (str "/api/case/" case-id "/" (name asset-type) "/" (name asset-id) "/notification-docx")
                    ;:download (str case-reference " - " (name asset-id) " - notification.docx")
                    :variant :outlined
                    :full-width true
                    :startIcon (r/as-element [ui/icon-download])}
        "download current letter"]
       [mui/button ;shared/upload-button asset-type case-id asset-id
        {:variant :outlined
         :full-width true
         :startIcon (r/as-element [ui/icon-upload])}
        "upload replacement"]]]]))

(defn pdf-viewer [props]
  (r/with-let [loading? (r/atom true)]
    [:<>
     [mui/box {:sx (merge {:mt 2
                           :text-align :center}
                          (when-not @loading?
                            {:display :none}))}
      [mui/circular-progress]]
     [:iframe (merge props
                     {:frameBorder 0
                      :onLoad #(reset! loading? false)}
                     (when @loading?
                       {:style {:display :none}}))]]))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        letter-id @(rf/subscribe [::model/open-letter-id])
        letter-data @(rf/subscribe [::model/open-letter])]
    [mui/stack {:sx {:height 1}}
     [mui/stack {:direction :row}
      [mui/icon-button {:onClick #(rf/dispatch [::model/close-letter])
                        :sx {:align-self :center
                             :ml 1}}
       [ui/icon-arrow-back-sharp]]
      [mui/list-item
       [mui/list-item-icon [ui/icon-description-outlined {:color :unset}]]
       [mui/list-item-text
        {:primary "notification letter"
         :secondary (str "generated from case data"
                         " (" (date-util/show-date-local-numeric (:modified-at letter-data)) ")")
         :sx {:flex-grow 0}}]]
      [mui/stack {:direction :row
                  :spacing 1
                  :sx {:align-self :center}}
       [mui/button {:variant :outlined
                    :startIcon (r/as-element [ui/icon-edit])
                    :endIcon (r/as-element [ui/icon-expand-more])
                    :onClick #(reset! edit-anchor (ui/event-currentTarget %))}
        "edit"]
       [edit-popover]
       [mui/button {:variant :contained
                    :endIcon (r/as-element [ui/icon-send])}
        "send"]
       [send-confirmation-dialog]]]
     [pdf-viewer {:style {:flex-grow 1}
                  :src (str "/api/case/" case-id "/notification-letter/" letter-id "/pdf")}]]))
