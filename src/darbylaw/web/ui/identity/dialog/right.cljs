(ns darbylaw.web.ui.identity.dialog.right
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.identity.model :as model]
    [darbylaw.web.ui.identity.alert :as alert]
    [darbylaw.web.ui.identity.dialog.utils :refer [check-icon]]
    [re-frame.core :as rf]
    [reagent.core :as r]))


(defn check-row [title {:keys [ssid result status final-result dashboard]}]
  [mui/table-row
   [mui/table-cell
    (when final-result
      [mui/tooltip {:title (or result status)}
       [:div
        [check-icon final-result]]])]
   [mui/table-cell
    title]
   [mui/table-cell
    [mui/link {:href dashboard :target :_blank} ssid]]])

(defn override-button [case-id]
  (r/with-let [open? (r/atom false)
               open-menu #(reset! open? true)
               close-menu #(reset! open? false)
               id (str (gensym "override-button-"))]
    [:<>
     [mui/button {:id id
                  :variant :outlined
                  :on-click open-menu
                  :end-icon (r/as-element 
                              (if @open?
                                [ui/icon-keyboard-arrow-up]
                                [ui/icon-keyboard-arrow-down]))}
      "Override"]
     [mui/menu {:open @open?
                :on-close close-menu
                :anchor-el (js/document.querySelector (str "#" id))}
      [mui/menu-item {:on-click #(do (close-menu)
                                     (rf/dispatch [::model/set-override-result case-id :pass]))
                      :style {:min-width 120}}
       [mui/list-item-icon {:style {:color "green"}}
        [ui/icon-check]]
       [mui/list-item-text {:style {:color "green"}}
        "pass"]]
      [mui/menu-item {:on-click #(do (close-menu)
                                     (rf/dispatch [::model/set-override-result case-id :fail]))
                      :style {:min-width 120}}
       [mui/list-item-icon {:style {:color "red"}}
        [ui/icon-warning]]
       [mui/list-item-text {:style {:color "red"}}
        "fail"]]]]))

(defn check-table []
  [mui/table
   [mui/table-head
    [mui/table-row
     [mui/table-cell] ; Result/status
     [mui/table-cell "Check"]
     [mui/table-cell "SSID"]]]
   [mui/table-body
    [check-row "UK AML"
     @(rf/subscribe [::model/uk-aml])]
    [check-row "Fraud Check"
     @(rf/subscribe [::model/fraudcheck])]
    [check-row "SmartDoc Check"
     @(rf/subscribe [::model/smartdoc])]]])

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-ref @(rf/subscribe [::case-model/current-case-reference])
        override @(rf/subscribe [::model/override-result])]
    [mui/stack {:spacing 1
                :width "50%"}
     [alert/dialog]
     [mui/stack {:direction :row
                 :spacing 2
                 :align-items :center}
      [override-button case-id]
      [mui/collapse {:in (boolean override)
                     :orientation :horizontal}
       [mui/stack {:direction :row
                   :align-items :center
                   :min-width "50px"}
        (case override
          :pass [mui/typography {:color :green}
                 "pass"]
          :fail [mui/typography {:color :red}
                 "fail"]
          ;; HACK: Make the text transparent so that the width doesn't jankily change
          ;;       Also use a four letter word like the others
          [mui/typography {:color :transparent}
           "four"])
        [mui/icon-button {:on-click #(rf/dispatch [::model/set-override-result case-id nil])}
         [ui/icon-refresh]]]]
      [mui/box {:flex-grow 1}]
      [mui/icon-button {:on-click #(rf/dispatch [::alert/set-alert-dialog-open {:case-id case-id}])
                        :disabled @(rf/subscribe [::model/submitting?])}
       [ui/icon-playlist-play]]
      (when @(rf/subscribe [::model/has-checks?])
        (let [{aml-report :report} @(rf/subscribe [::model/uk-aml])
              {smartdoc-report :report} @(rf/subscribe [::model/smartdoc])
              show? (or aml-report smartdoc-report)
              partial? (not (and aml-report smartdoc-report))]
          (when show?
            [mui/button {:href (str "/api/case/" case-id "/identity-checks/download-pdf")
                         :download (str case-ref ".identity."
                                        (if partial?
                                          "partial-report"
                                          "full-report")
                                        ".pdf")}
             (if partial?
               "partial report"
               "full report")])))]
     (if-not @(rf/subscribe [::model/has-checks?])
       [mui/table-row
        [mui/table-cell {:col-span 5}
         [mui/alert {:severity :info}
          [mui/alert-title "No checks run"]
          (if-not @(rf/subscribe [::model/submitting?])
           [mui/typography
            "Click " 
            [mui/link {:on-click #(rf/dispatch [::alert/set-alert-dialog-open {:case-id case-id}])
                       :style {:cursor :pointer}}
             "here"]
            " to run the checks."]
           [mui/typography
            "Running checks..."])]]]
       [check-table])]))
