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
    {:dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::check-submit-failure
  (fn [{:keys [db]} [_ error-result]]
    {::ui/notify-user-http-error {:message "Error starting identity checks"
                                  :result error-result}}))

(rf/reg-event-fx ::identity-check
  (fn [{:keys [db]} [_ case-id]]
    {:http-xhrio
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
        :fail [ui/icon-error-outline {:style {:color "red"}}]
        :refer [ui/icon-error-outline {:style {:color "orange"}}]
        :pass [ui/icon-check {:style {:color "green"}}]
        :unknown [ui/icon-directions-run {:style {:color "grey"}}]
        :processing [ui/icon-directions-run {:style {:color "grey"}}])])))

(defn check-row [title {:keys [ssid final-result dashboard]}]
  [mui/table-row
   [mui/table-cell
    [check-icon final-result]]
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
                      :style {:min-width 120
                              :color "green"}}
       "pass"]
      [mui/menu-item {:on-click #(do (close-menu)
                                     (rf/dispatch [::set-override-result case-id :fail]))
                      :style {:min-width 120
                              :color "red"}}
       "fail"]
      [mui/menu-item {:on-click #(do (close-menu)
                                     (rf/dispatch [::set-override-result case-id nil]))
                      :style {:min-width 120}}
       "unset"]]]))

(def base-url "https://sandbox.smartsearchsecure.com")
(defn content []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        case-ref @(rf/subscribe [::case-model/current-case-reference])]
    [mui/stack {:spacing 1}
     [mui/stack {:direction :row
                 :spacing 2
                 :align-items :center}
      [override-button case-id]
      (when-let [override @(rf/subscribe [::model/override-result])]
        [mui/typography
         (case override
           :pass [mui/typography {:color "green"}
                  "pass"]
           :fail [mui/typography {:color "red"}
                  "fail"])])
      [mui/box {:flex-grow 1}]
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
     [mui/table
      [mui/table-head
       [mui/table-row
        [mui/table-cell] ; Result/status
        [mui/table-cell "Check"]
        [mui/table-cell "SSID"]]]
      [mui/table-body
       (if-not @(rf/subscribe [::model/has-checks?])
         [mui/table-row
          [mui/table-cell {:col-span 5}
           [mui/alert {:severity :info}
            [mui/alert-title "No checks run"]
            "Click " 
            [mui/link {:on-click #(rf/dispatch [::identity-check case-id])
                       :style {:cursor :pointer}}
             "here"]
            " to run the checks."]]]
         [:<>
          [check-row "UK AML"
           @(rf/subscribe [::model/uk-aml])]
          [check-row "Fraud Check"
           @(rf/subscribe [::model/fraudcheck])]
          [check-row "SmartDoc Check"
           @(rf/subscribe [::model/smartdoc])]])]]]))

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))}
   [mui/stack {:spacing 1}
    [mui/dialog-title
     [mui/stack {:spacing 1 :direction :row}
      "identity checks"
      [mui/box {:flex-grow 1}]
      [ui/icon-close {:on-click #(rf/dispatch [::set-dialog-open])}]]]
    [mui/dialog-content
     [content]]]])
