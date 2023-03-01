(ns darbylaw.web.ui.identity.dialog
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.identity.model :as model]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::checks-submitting? false)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::check-submit-failure
  (fn [{:keys [db]} [_ error-result]]
    {:db (assoc db ::checks-submitting? false)
     ::ui/notify-user-http-error {:message "Error starting identity checks"
                                  :result error-result}}))

(rf/reg-event-fx ::identity-check
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::checks-submitting? true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity-checks/run")
        :on-success [::submit-success case-id]
        :on-failure [::check-submit-failure]})}))

(rf/reg-event-fx ::override-submit-failure
  (fn [{:keys [db]} [_ error-result]]
    {::ui/notify-user-http-error {:message "Error overriding result"
                                  :result error-result}}))

(rf/reg-event-fx ::set-override-result
  (fn [_ [_ case-id new-result]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity-checks/override")
        :url-params (when new-result
                      {:new-result (name new-result)})
        :on-success [::submit-success case-id]
        :on-failure [::override-submit-failure]})}))

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::dialog-open? true
                 ::dialog-context dialog-context})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::checks-submitting?
  (fn [db]
    (::checks-submitting? db)))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (::dialog-open? db)))

(defn check-icon
  ([]
   [check-icon @(rf/subscribe [::model/current-final-result])])
  ([result]
   (r/as-element
     [:a {:style {:line-height 0
                  :cursor :pointer}
          :on-click #(rf/dispatch [::set-dialog-open {}])}
      (case result
        :unknown [ui/icon-playlist-play {:style {:color "grey"}}]
        :processing [ui/icon-pending {:style {:color "grey"}}]
        :pass [ui/icon-check {:style {:color "green"}}]
        :refer [ui/icon-warning-amber {:style {:color "orange"}}]
        :fail [ui/icon-warning {:style {:color "red"}}])])))

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
                                     (rf/dispatch [::set-override-result case-id :pass]))
                      :style {:min-width 120}}
       [mui/list-item-icon {:style {:color "green"}}
        [ui/icon-check]]
       [mui/list-item-text {:style {:color "green"}}
        "pass"]]
      [mui/menu-item {:on-click #(do (close-menu)
                                     (rf/dispatch [::set-override-result case-id :fail]))
                      :style {:min-width 120}}
       [mui/list-item-icon {:style {:color "red"}}
        [ui/icon-warning]]
       [mui/list-item-text {:style {:color "red"}}
        "fail"]]]]))

(rf/reg-sub ::alert-dialog-open?
  (fn [db]
    (::alert-dialog-open? db)))

(rf/reg-event-db ::set-alert-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::alert-dialog-open? true
                 ::alert-dialog-context dialog-context})
      (assoc db ::alert-dialog-open? false))))

(rf/reg-event-fx ::alert-confirm
  (fn [{:keys [db]} _]
    {:fx [[:dispatch [::set-alert-dialog-open nil]]
          [:dispatch [::identity-check (get-in db [::alert-dialog-context :case-id])]]]}))

(defn alert-dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::alert-dialog-open?]))
               :max-width :xs}
   [mui/dialog-title "Are you sure?"]
   [mui/dialog-content "Continuing will perform another set of checks and ask the user to submit another set of documents."]
   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :full-width true
                 :on-click #(rf/dispatch [::set-alert-dialog-open nil])}
     "Cancel"]
    [mui/button {:variant :contained
                 :full-width true
                 :color :primary
                 :on-click #(rf/dispatch [::alert-confirm])}
     "Yes"]]])

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

(defn content []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-ref @(rf/subscribe [::case-model/current-case-reference])
        override @(rf/subscribe [::model/override-result])]
    [mui/stack {:spacing 1}
     [alert-dialog]
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
        [mui/icon-button {:on-click #(rf/dispatch [::set-override-result case-id nil])}
         [ui/icon-refresh]]]]
      [mui/box {:flex-grow 1}]
      [mui/icon-button {:on-click #(rf/dispatch [::set-alert-dialog-open {:case-id case-id}])
                        :disabled @(rf/subscribe [::checks-submitting?])}
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
          (if-not @(rf/subscribe [::checks-submitting?])
           [mui/typography
            "Click " 
            [mui/link {:on-click #(rf/dispatch [::set-alert-dialog-open {:case-id case-id}])
                       :style {:cursor :pointer}}
             "here"]
            " to run the checks."]
           [mui/typography
            "Running checks..."])]]]
       [check-table])]))

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))}
   [mui/backdrop {:open (boolean @(rf/subscribe [::checks-submitting?]))}
    [mui/circular-progress]]
   [mui/stack {:spacing 1}
    [mui/dialog-title
     [mui/stack {:spacing 1 :direction :row}
      "identity checks"
      [mui/box {:flex-grow 1}]
      [ui/icon-close {:on-click #(rf/dispatch [::set-dialog-open])}]]]
    [mui/dialog-content
     [content]]]])
