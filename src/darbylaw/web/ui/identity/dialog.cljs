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

(rf/reg-event-fx ::identity-check
  (fn [{:keys [db]} [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity")
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure]})}))

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

(defn check-row [title ssid result link]
  [mui/table-row
   [mui/table-cell
    [check-icon result]]
   [mui/table-cell
    title]
   [mui/table-cell
    [mui/link {:href link :target :_blank} ssid]]])

(def base-url "https://sandbox.smartsearchsecure.com")
(defn content []
  [mui/stack {:spacing 1}
   [mui/table
    [mui/table-head
     [mui/table-row
      [mui/table-cell] ; Status
      [mui/table-cell "Check"]
      [mui/table-cell "SSID"]]]
    [mui/table-body
     (if-not @(rf/subscribe [::model/has-checks?])
       (let [case-id @(rf/subscribe [::case-model/case-id])]
         [mui/table-row
          [mui/table-cell {:col-span 5}
           [mui/alert {:severity :info}
            [mui/alert-title "No checks run"]
            "Click " 
            [mui/link {:on-click #(rf/dispatch [::identity-check case-id])
                       :style {:cursor :pointer}}
             "here"]
            " to run the checks."]]])
       [:<>
        (let [uk-aml @(rf/subscribe [::model/uk-aml])]
          [check-row "UK AML"
           (:ssid uk-aml)
           (:final-result uk-aml)
           (str base-url "/aml/results/" (:ssid uk-aml))])
        (let [fraudcheck @(rf/subscribe [::model/fraudcheck])]
          [check-row "Fraud Check"
           (:ssid fraudcheck)
           (:final-result fraudcheck)
           (str base-url "/aml/results/" (:ssid fraudcheck))])
        (let [smartdoc @(rf/subscribe [::model/smartdoc])]
          [check-row "SmartDoc Check"
           (:ssid smartdoc)
           (:final-result smartdoc)
           (str base-url "/doccheck/results/" (:ssid smartdoc))])])]]])

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
