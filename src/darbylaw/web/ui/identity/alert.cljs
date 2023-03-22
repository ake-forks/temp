(ns darbylaw.web.ui.identity.alert
  (:require
    [reagent-mui.components :as mui]

    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.identity.model :as model]))

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
          [:dispatch [::model/identity-check (get-in db [::alert-dialog-context :case-id])]]]}))

(defn dialog []
  [mui/dialog {:open (boolean @(rf/subscribe [::alert-dialog-open?]))
               :max-width :xs}
   [mui/dialog-title "Run Checks"]
   [mui/dialog-content
    (let [nickname @(rf/subscribe [::case-model/nickname])]
      [:<>
       [mui/typography
         "Continuing will:"]
       [mui/list {:sx {:list-style-type :disc
                       :list-style-position :inside}}
        [mui/list-item {:sx {:display :list-item}}
         (str "Perform a set of checks on " nickname)]
        [mui/list-item {:sx {:display :list-item}}
         (str "Ask " nickname " to submit a set of documents")]]
       (when @(rf/subscribe [::model/has-checks?])
         [mui/typography
           "If you want to re-run a single check, use the SmartSearch UI by clicking on an SSID"])
       (when @(rf/subscribe [::case-model/fake?])
         [mui/alert {:severity :info
                     :sx {:mt 2}}
          "This is a fake case. Checks will run in a sandbox SmartSearch environment."])])]
   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :full-width true
                 :on-click #(rf/dispatch [::set-alert-dialog-open nil])}
     "Cancel"]
    [mui/button {:variant :contained
                 :full-width true
                 :color :primary
                 :on-click #(rf/dispatch [::alert-confirm])}
     "Run"]]])
