(ns darbylaw.web.ui.banking.stage-notify
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.shared :as shared]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))

(def review-result (r/atom nil))
(def override-fake-send? (r/atom false))

(defn approve-options []
  (r/with-let [_ (reset! review-result nil)]
    [mui/box {:sx {:mt 2}}
     [mui/typography {:variant :body1}
      "Approve letter and send:"]
     [mui/radio-group {:value @review-result
                       :onChange (fn [_ev value]
                                   (let [value (keyword value)]
                                     (reset! review-result value)
                                     (when (= value :do-not-send)
                                       (reset! override-fake-send? false))))}
      [mui/form-control-label
       {:value :send
        :label "I approve, the letter is ready to be sent."
        :control (r/as-element [mui/radio])}]
      [mui/form-control-label
       {:value :do-not-send
        :label "Do not send, bank will be notified by other means."
        :control (r/as-element [mui/radio])}]]
     #_[mui/alert {:severity :info
                   :sx (merge
                         {:mt 2}
                         (when (= @review-result :do-not-send)
                           {:visibility :hidden}))}
        "The letter will be posted automatically through ordinary mail. "
        "We will wait for an answer from the bank for the final valuation."]]))

(defn fake-options []
  (r/with-let [_ (reset! override-fake-send? false)]
    (let [fake? @(rf/subscribe [::case-model/fake?])]
      (when fake?
        [mui/box {:sx {:mt 2}}
         [mui/typography {:sx {:mb 1}}
          "This is a " [:b "fake"] " case, and therefore no real letter will be posted. "
          "You can override that for testing purposes:"]
         [mui/form-control-label
          {:checked @override-fake-send?
           :onChange (fn [_ev checked] (reset! override-fake-send? checked))
           :control (r/as-element [mui/switch])
           :disabled (not= @review-result :send)
           :label "Post a real letter!"}]
         (when @override-fake-send?
           [mui/alert {:severity :warning}
            "Ensure there is a proper test address on the letter!"])]))))

(defn submit-buttons [case-id asset-id]
  (let [type @(rf/subscribe [::model/get-type])
        letter-id @(rf/subscribe [::model/notification-letter-id])
        fake? @(rf/subscribe [::case-model/fake?])
        send-action (case @review-result
                      :send (if (and fake? (not @override-fake-send?))
                              :fake-send
                              :send)
                      :do-not-send :do-not-send
                      nil :disabled)]
    [mui/stack {:spacing 1
                :direction :row
                :justify-content :space-between
                :sx {:width 1}}
     [mui/button {:onClick #(rf/dispatch [::model/hide-dialog])
                  :variant :contained
                  :full-width true}
      "cancel"]
     [mui/button {:on-click #(rf/dispatch [::model/review-notification-letter
                                           type send-action case-id asset-id letter-id])
                  :variant :contained
                  :endIcon (case @review-result
                             :do-not-send (r/as-element [ui/icon-arrow-forwards])
                             (r/as-element [ui/icon-send]))
                  :disabled (= send-action :disabled)
                  :full-width true}
      (case @review-result
        :do-not-send "Skip send"
        "Send letter")]]))

(def regenerating? (r/atom false))

(ui/reg-fx+event ::reset-regenerating
  (fn [_]
    (reset! regenerating? false)))

(rf/reg-event-fx ::regenerate-finished
  (fn [_ [_ case-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id
                      {:on-success [::reset-regenerating]}]]]}))

(rf/reg-event-fx ::regenerate
  (fn [_ [_ type case-id asset-id letter-id]]
    (reset! regenerating? true)
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" type "/" (name asset-id)
               "/notification-letter/" letter-id "/regenerate")
        :on-success [::regenerate-finished case-id]
        :on-failure [::regenerate-finished case-id]})}))

(defn control-buttons []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-reference [::case-model/current-case-reference]
        asset-id @(rf/subscribe [::model/current-asset-id])
        type @(rf/subscribe [::model/get-type])]
    [mui/stack {:direction :row :spacing 1}
     [mui/button {:href (str "/api/case/" case-id "/" type "/" (name asset-id) "/notification-docx")
                  :download (str case-reference " - " (name asset-id) " - notification.docx")
                  :variant :outlined
                  :full-width true
                  :startIcon (r/as-element [ui/icon-download])}
      "download current letter"]
     [shared/upload-button type case-id asset-id
      {:variant :outlined
       :full-width true
       :startIcon (r/as-element [ui/icon-upload])}
      "upload replacement"
      "/notification-docx"]
     ;TODO replace "use docx file type" with specific error
     [model/upload-error-snackbar "Please upload a docx file type."]]))

(defn approve-notification []
  (r/with-let [_ (reset! model/file-uploading? false)]
    (let [asset-id @(rf/subscribe [::model/current-asset-id])
          case-id @(rf/subscribe [::case-model/case-id])
          case-reference @(rf/subscribe [::case-model/current-case-reference])
          asset-type @(rf/subscribe [::model/get-type])
          letter-id @(rf/subscribe [::model/notification-letter-id])
          author @(rf/subscribe [::model/author])]
      [:<>
       [mui/backdrop {:open (or @model/file-uploading?
                              @regenerating?)}]
       [mui/typography {:variant :body1
                        :font-weight :bold}
        (cond
          (= author :unknown-user)
          "This notification letter was modified by a user."

          (string? author)
          (str "This notification letter was modified by '" author "'.")

          :else
          "This notification letter was automatically generated from case data. Please review.")]
       [ui/loading-button {:onClick #(rf/dispatch [::regenerate asset-type case-id asset-id letter-id])
                           :loading @regenerating?
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
        [mui/button {:href (str "/api/case/" case-id "/" asset-type "/" (name asset-id) "/notification-docx")
                     :download (str case-reference " - " (name asset-id) " - notification.docx")
                     :variant :outlined
                     :full-width true
                     :startIcon (r/as-element [ui/icon-download])}
         "download current letter"]
        [shared/upload-button asset-type case-id asset-id
         {:variant :outlined
          :full-width true
          :startIcon (r/as-element [ui/icon-upload])}
         "upload replacement"
         "/notification-docx"]]
       [approve-options]
       [fake-options]])))

(defn panel []
  (let [asset-id (:id @(rf/subscribe [::model/get-dialog]))
        case-id @(rf/subscribe [::case-model/case-id])
        type @(rf/subscribe [::model/get-type])]
    (if (some? asset-id)
      [mui/box shared/tall-dialog-props
       [mui/stack {:spacing 1
                   :direction :row
                   :sx {:height 1}}
        ;left side
        [mui/stack {:spacing 1 :sx {:width 0.5}}
         (if @model/file-uploading?
           [reagent-mui.lab.loading-button/loading-button {:loading true :full-width true}]
           [:iframe {:style {:height "100%"}
                     :src (str "/api/case/" case-id "/" type "/" (name asset-id) "/notification-pdf")}])]
        ;right side
        [mui/stack {:spacing 1 :sx {:width 0.5}}
         [mui/dialog-title
          [shared/header type asset-id :notify]]
         [mui/dialog-content
          [approve-notification]]
         [mui/dialog-actions
          [submit-buttons case-id asset-id]]]]])))