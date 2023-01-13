(ns darbylaw.web.ui.banking.stage-notify
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.banking.shared :as shared]
    [darbylaw.web.ui.banking.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))


(def approved? (r/atom false))
(def review-result (r/atom nil))
(def fake-send? (r/atom @(rf/subscribe [::case-model/fake?])))
(defn submit-buttons [case-id asset-id]
  (let [type @(rf/subscribe [::model/get-type])
        letter-id (model/get-letter-id type)]
    [mui/stack {:spacing 1
                :direction :row
                :justify-content :space-between
                :sx {:width 1}}
     [mui/button {:onClick #(do (reset! approved? false)
                                (rf/dispatch [::model/hide-dialog]))
                  :variant :contained
                  :full-width true}
      "cancel"]
     [mui/button {:on-click #(let [send-action (case @review-result
                                                 :send (if @fake-send?
                                                         :fake-send
                                                         :send)
                                                 :do-not-send :do-not-send)]
                               (rf/dispatch [::model/review-notification-letter type send-action case-id asset-id letter-id]))
                  :variant :contained
                  :startIcon (case @review-result
                               :do-not-send (r/as-element [ui/icon-arrow-forwards])
                               (r/as-element [ui/icon-send]))
                  :disabled (nil? @review-result)
                  :full-width true}
      (case @review-result
        :do-not-send "Skip send"
        "Send letter")]]))

(rf/reg-event-fx ::reset-regenerating
  (fn [_ _]
    (reset! model/file-uploading? false)))

(rf/reg-event-fx ::regenerate--success
  (fn [{:keys [db]} [_ case-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id
                      {:on-success [::reset-regenerating]}]]]}))
(rf/reg-event-fx ::regenerate
  (fn [{:keys [db]} [_ type case-id asset-id letter-id]]
    (reset! model/file-uploading? true)
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/" type "/" (name asset-id)
               "/notification-letter/" letter-id "/regenerate")
        :on-success [::regenerate--success case-id]
        :on-failure [::reset-regenerating]})}))

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

(defn approval []
  [mui/alert {:severity :info}
   [mui/stack
    "The letter will be posted automatically through ordinary mail. "
    "We will wait for an answer from the bank for the final valuation step."]
   [mui/form-control-label
    {:control (r/as-element
                [mui/checkbox
                 {:checked @approved?
                  :on-change #(reset! approved? (not @approved?))}])
     :label "I approve, the letter is ready to be sent."}]])



(def active-step-label-props
  {:StepIconComponent (react-component [props]
                        [mui/step-icon
                         (merge props
                           {:active true})])})
(defn approve-notification []
  (r/with-let [_ (reset! model/file-uploading? false)
               asset-id @(rf/subscribe [::model/current-asset-id])
               case-id @(rf/subscribe [::case-model/case-id])
               case-reference @(rf/subscribe [::case-model/current-case-reference])
               asset-type @(rf/subscribe [::model/get-type])
               asset-data (model/get-asset-data type)
               letter-id (model/get-letter-id type)
               fake? @(rf/subscribe [::case-model/fake?])
               author (model/get-author type)]

    [mui/box
     [mui/stepper {:orientation :vertical
                   :nonLinear true
                   :activeStep 100}
      [mui/step {:key :view
                 :expanded true}
       [mui/step-label active-step-label-props
        "Review letter"]
       [mui/step-content
        [mui/typography {:variant :body1
                         :font-weight :bold}
         (cond
           (= author :unknown-user)
           "This letter was modified by a user."

           (string? author)
           (str "This letter was uploaded by " author)

           :else
           "This letter was automatically generated from case data.")]
        [ui/loading-button {:onClick #(rf/dispatch [::regenerate asset-type case-id asset-id letter-id])
                            :loading @model/file-uploading?
                            :startIcon (r/as-element [ui/icon-refresh])
                            :variant :outlined
                            :sx {:mt 1}}
         "Regenerate letter from case data"]]]
      [mui/step {:key :edit
                 :expanded true}
       [mui/step-label active-step-label-props
        "Modify letter if needed"]
       [mui/step-content
        [mui/typography {:variant :body1}
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
          "/notification-docx"]]]]
      [mui/step {:key :approve
                 :expanded true}
       [mui/step-label active-step-label-props
        "Approve and send"]
       [mui/step-content
        [mui/radio-group {:value @review-result
                          :onChange (fn [_ev value]
                                      (reset! review-result (keyword value)))}
         [mui/form-control-label
          {:value :send
           :label "I approve, the letter is ready to be sent."
           :control (r/as-element [mui/radio])}]
         [mui/form-control-label
          {:value :do-not-send
           :label "Do not send, bank will be notified by other means."
           :control (r/as-element [mui/radio])}]]

        [mui/alert {:severity :info
                    :sx (merge
                          {:mt 2}
                          (when (= @review-result :do-not-send)
                            {:visibility :hidden}))}
         "The letter will be posted automatically through ordinary mail. "
         "We will wait for an answer from the bank for the final valuation step."]]]]]))







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