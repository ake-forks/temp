(ns darbylaw.web.ui.notification.letter
  (:require [darbylaw.api.util.http :as http]
            [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.util.date :as date-util]
            [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.notification.model :as model]
            [darbylaw.web.ui.components.upload-button :refer [upload-button]]
            [darbylaw.web.ui.notification.letter-commons :refer [letter-header]]
            [darbylaw.web.ui.components.pdf-viewer :refer [pdf-viewer]]))

(defonce delete-confirmation-open? (r/atom false))

(defn delete-confirmation-dialog []
  [mui/dialog {:open (boolean @delete-confirmation-open?)
               :maxWidth :sm}
   [mui/dialog-title "confirm delete"]
   [mui/dialog-content
    "Do you want to delete the letter?"]
   [mui/dialog-actions
    [mui/button {:variant :contained
                 :onClick #(reset! delete-confirmation-open? false)}
     "No, cancel"]
    [mui/button {:variant :outlined
                 :color :error
                 :onClick (let [case-id (<< ::case-model/case-id)
                                letter-id (<< ::model/open-letter-id)]
                            (fn []
                              (rf/dispatch [::model/delete-letter
                                            {:case-id case-id
                                             :letter-id letter-id
                                             :on-completed
                                             #(reset! delete-confirmation-open? false)}])))}
     "Yes, delete"]]])

(defonce confirmation-dialog-open? (r/atom false))
(defonce contents-approved? (r/atom false))
(defonce override-fake-send? (r/atom false))

(defn open-confirmation-dialog! []
  (reset! confirmation-dialog-open? true)
  (reset! contents-approved? false)
  (reset! override-fake-send? false))

(defn send-confirmation-dialog []
  [mui/dialog {:open (boolean @confirmation-dialog-open?)
               :maxWidth :sm}
   [mui/dialog-title "confirm send"]
   [mui/dialog-content
    [mui/stack {:spacing 1}
     [mui/typography
      "Approve letter contents:"]
     [mui/form-control-label
      {:checked @contents-approved?
       :onChange (fn [_ev checked] (reset! contents-approved? checked))
       :control (r/as-element [mui/checkbox])
       :label "I approve, the letter is ready to be sent."}]
     (when (<< ::case-model/fake?)
       [mui/accordion {:variant :outlined}
        [mui/accordion-summary {:expandIcon (r/as-element [ui/icon-expand-more])}
         [mui/typography {:variant :body2}
          "This is a " [:b "fake"] " case, and therefore "
          [:b "no real letter will be posted"] "."]]
        [mui/accordion-details
         "You can override that for testing purposes:"
         [mui/form-control-label
          {:checked @override-fake-send?
           :onChange (fn [_ev checked] (reset! override-fake-send? checked))
           :control (r/as-element [mui/switch])
           ;:disabled (not= @review-result :send)
           :label "Post a real letter!"}]
         [mui/alert (merge {:severity :warning}
                           (when-not @override-fake-send?
                             {:sx {:visibility :hidden}}))
          "Make sure that there is a proper test address on the letter!"]]])]]
   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :onClick #(reset! confirmation-dialog-open? false)}
     "Cancel"]
    [mui/button {:variant :contained
                 :color :error
                 :disabled (not @contents-approved?)
                 :onClick (let [case-id (<< ::case-model/case-id)
                                letter-id (<< ::model/open-letter-id)
                                fake-case? (<< ::case-model/fake?)]
                            #(do (reset! confirmation-dialog-open? false)
                                 (rf/dispatch [::model/send-letter
                                               {:case-id case-id
                                                :letter-id letter-id
                                                :fake (and fake-case?
                                                           (not @override-fake-send?))}])))}
     "Send"]]])

(defonce edit-anchor (r/atom nil))
(defonce upload-filename (r/atom nil))
(defonce file-uploading? (r/atom false))
(defonce pdf-viewer-refresh (r/atom 0))
(defn refresh-pdf-viewer! []
  (swap! pdf-viewer-refresh inc))

(defn edit-popover []
  (let [case-id (<< ::case-model/case-id)
        case-reference (<< ::case-model/current-case-reference)
        letter-id (<< ::model/open-letter-id)
        close! #(reset! edit-anchor nil)]
    [mui/popover {:open (some? @edit-anchor)
                  :anchorEl @edit-anchor
                  :onClose close!
                  :anchorOrigin {:vertical :bottom
                                 :horizontal :right}
                  :transformOrigin {:vertical :top
                                    :horizontal :right}
                  :PaperProps {:sx {:border-radius 0}}}
     [mui/container {:maxWidth :xs
                     :sx {:my 2}}
      [mui/stack {:spacing 2}
       [mui/stack
        [mui/typography
         "You can modify the letter using Word."]
        [mui/typography {:variant :body2}
         "(Be careful in keeping the first page layout intact, "
         "as the address must match the envelope's window)."]]
       [mui/stack {:spacing 1
                   :align-items :flex-start}
        [mui/button {:href (str "/api/case/" case-id "/notification-letter/" letter-id "/docx")
                     :download (str case-reference " - " letter-id " - notification.docx")
                     :variant :outlined
                     :startIcon (r/as-element [ui/icon-download])}
         "download current letter"]
        [upload-button
         {:button-props {:variant :outlined
                         :startIcon (r/as-element [ui/icon-upload])}
          :input-props {:accept http/docx-mime-type}
          :filename-atom upload-filename
          :uploading?-atom file-uploading?
          :on-file-selected (fn [file]
                              (rf/dispatch [::model/replace-letter
                                            {:case-id case-id
                                             :letter-id letter-id
                                             :file file
                                             :on-completed
                                             #(do (reset! file-uploading? false)
                                                  (refresh-pdf-viewer!)
                                                  (close!))}]))}
         "upload replacement"]]
       [mui/divider]
       [mui/typography
        "You can also dismiss this letter:"]
       [mui/button {:variant :outlined
                    :startIcon (r/as-element [ui/icon-delete])
                    :onClick (fn []
                               (reset! delete-confirmation-open? true)
                               (close!))
                    :sx {:align-self :flex-start}}
        "delete letter"]]]]))

(defn panel []
  (let [case-id (<< ::case-model/case-id)
        letter-id (<< ::model/open-letter-id)
        letter-data (<< ::model/open-letter)]
    [mui/stack {:sx {:height 1}}
     [letter-header {:on-back #(rf/dispatch [::model/close-letter])}
      [mui/list-item
       [mui/list-item-icon [ui/icon-description-outlined {:color :unset}]]
       [mui/list-item-text
        {:primary "notification letter"
         :secondary (str (let [author (:author letter-data)]
                           (cond
                             (= author :unknown-user) "modified by a user"
                             (string? author) (str "modified by '" author "'")
                             :else "generated from case data"))
                         " (" (date-util/show-date-local-numeric (:modified-at letter-data)) ")")
         :sx {:flex-grow 0}}]]
      (when (<< ::model/letter-in-preparation?)
        [mui/stack {:direction :row
                    :spacing 1
                    :sx {:align-self :center}}
         [mui/button {:variant :outlined
                      :startIcon (r/as-element [ui/icon-edit])
                      :endIcon (r/as-element [ui/icon-expand-more])
                      :onClick #(reset! edit-anchor (ui/event-currentTarget %))}
          "edit"]
         [edit-popover]
         [delete-confirmation-dialog]
         [mui/button {:variant :contained
                      :endIcon (r/as-element [ui/icon-send])
                      :onClick #(open-confirmation-dialog!)}
          "send"]
         [send-confirmation-dialog]])]
     ^{:key @pdf-viewer-refresh}
     [pdf-viewer {:sx {:flex-grow 1}
                  :src (str "/api/case/" case-id "/notification-letter/" letter-id "/pdf")}]]))
