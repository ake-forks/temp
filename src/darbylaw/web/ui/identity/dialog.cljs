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

(defn check-icon []
  (let [final-result @(rf/subscribe [::model/current-final-result])]
    (r/as-element
      [:a {:style {:line-height 0
                   :cursor :pointer}
           :on-click #(rf/dispatch [::set-dialog-open {}])}
       (case final-result
         :fail [ui/icon-error-outline {:style {:color "red"}}]
         :refer [ui/icon-error-outline {:style {:color "orange"}}]
         :pass [ui/icon-check {:style {:color "green"}}]
         :unknown [ui/icon-directions-run {:style {:color "grey"}}])])))

(defn content []
  (let [final-result @(rf/subscribe [::model/current-final-result])]
    [mui/stack {:spacing 1}
     (if (= :unknown final-result)
       (let [case-id @(rf/subscribe [::case-model/case-id])]
         [:<>
          [mui/typography
           "No checks have been run for this user."]
          [mui/typography
           "Press the button below to run the checks."]
          [mui/button {:on-click #(rf/dispatch [::identity-check case-id])
                       :variant "contained"}
           "Run"]])
       (let [checks @(rf/subscribe [::model/current-checks])]
         [:<>
          [mui/typography {:variant :h5}
           "Checks:"]
          [mui/table
           [mui/table-head
            [mui/table-row
             [mui/table-cell]
             [mui/table-cell "Check"]
             [mui/table-cell "SSID"]
             [mui/table-cell "Status"]
             [mui/table-cell "Result"]]]
           [mui/table-body
            (for [{:keys [type status result ssid]} checks]
              ^{:key ssid}
              [mui/table-row
               [mui/table-cell
                [check-icon]]
               [mui/table-cell
                [mui/typography {:variant :h6}
                 (str type)]]
               [mui/table-cell
                [:a {:href (str "https://sandbox.smartsearchsecure.com/aml/results/" ssid)
                     :target :_blank}
                 ssid]]
               [mui/table-cell
                (if status
                  status
                  "?")]
               [mui/table-cell
                (if result
                  result
                  "?")]])]]]))]))

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))}
   [mui/stack {:spacing 1}
    [mui/dialog-title
     [mui/stack {:spacing 1 :direction :row}
      [mui/typography {:variant "h6"}
       "identity checks"]
      [mui/box {:flex-grow 1}]
      [ui/icon-close {:on-click #(rf/dispatch [::set-dialog-open])}]]]
    [mui/dialog-content
     [content]]]])
