(ns darbylaw.web.ui.bank-letter-approval
  (:require-macros [reagent-mui.util :refer [react-component]])
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-model :as bank-model]))

(def uploading? (r/atom false))

(rf/reg-event-fx ::load-case-success
  (fn [_ _]
    (reset! uploading? false)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    {:dispatch [::case-model/load-case! case-id
                {:on-success [::load-case-success]}]}))

(rf/reg-event-fx ::upload-failure
  (fn [_ _]
    (reset! uploading? false)))

(rf/reg-event-fx ::upload
  (fn [_ [_ case-id bank-id file]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/notification-docx")
        :body (doto (js/FormData.)
                (.append "file" file))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

(defn upload-button [_case-id _bank-id _props _label]
  (r/with-let [_ (reset! uploading? false)
               filename (r/atom "")]
    (fn [case-id bank-id props label]
      [ui/loading-button (merge props {:component "label"
                                       :loading @uploading?})
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::upload case-id bank-id selected-file])
                                (reset! filename "")
                                (reset! uploading? true))
                   :hidden true                             ;TODO not working as before. why?
                   :sx {:display :none}}]])))

(rf/reg-sub ::author
  :<- [::case-model/current-case]
  :<- [::bank-model/bank-id]
  (fn [[current-case bank-id]]
    (get-in current-case [:bank bank-id :notification-letter-author])))

(def active-step-label-props
  {:StepIconComponent (react-component [props]
                        [mui/step-icon
                         (merge props
                           {:active true})])})

(defn panel []
  (r/with-let [approved? (r/atom false)]
    (let [case-id @(rf/subscribe [::case-model/case-id])
          case-reference @(rf/subscribe [::case-model/current-case-reference])
          bank-id @(rf/subscribe [::bank-model/bank-id])
          bank-name @(rf/subscribe [::bank-model/bank-name])
          author @(rf/subscribe [::author])]
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
          [mui/button {:onClick #(rf/dispatch [::regenerate])
                       :startIcon (r/as-element [ui/icon-refresh])
                       :variant :outlined
                       :sx {:mt 1}}
           "Regenerate letter from case data"]
          #_[mui/stack {:direction :row
                        :alignItems :center
                        :spacing 1
                        :sx {:mt 2}}
             [mui/button {:variant :outlined
                          :startIcon (r/as-element
                                       (let [flip {:transform "scaleX(-1)"}]
                                         [ui/icon-launch {:sx flip}]))}
              "View letter here"]
             [mui/typography {:variant :body1}
              "or"]
             [mui/button {:variant :outlined
                          :startIcon (r/as-element [ui/icon-download])}
              "Download for review"]]]]
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
           [mui/button {:href (str "/api/case/" case-id "/bank/" (name bank-id) "/notification-docx")
                        :download (str case-reference " - " bank-name " - Bank notification.docx")
                        :variant :outlined
                        :startIcon (r/as-element [ui/icon-download])}
            "download current letter"]
           [upload-button case-id bank-id {:variant :outlined
                                           :startIcon (r/as-element [ui/icon-upload])}
            "upload replacement"]]]]
        [mui/step {:key :approve
                   :expanded true}
         [mui/step-label active-step-label-props
          "Approve and send"]
         [mui/step-content
          [mui/form-control-label
           {:control (r/as-element
                       [mui/checkbox {:checked @approved?
                                      :onChange #(reset! approved? (.. % -target -checked))}])
            :label "I approve, the letter is ready to be sent."}]
          [mui/alert {:severity :info}
           [mui/stack
            "The letter will be posted automatically through ordinary mail. "
            "We will wait for an answer from the bank for the final valuation step."]]]]]
       [mui/stack {:direction :row :spacing 1 :sx {:mt 1}}
        [mui/button {:onClick #(rf/dispatch [::bank-model/hide-bank-dialog])
                     :variant :contained
                     :full-width true} "cancel"]
        [mui/button {:on-click #(rf/dispatch [::bank-model/approve-notification-letter case-id bank-id])
                     :variant :contained
                     :startIcon (r/as-element [ui/icon-send])
                     :disabled (not @approved?)
                     :full-width true}
         "Send letter"]]])))


