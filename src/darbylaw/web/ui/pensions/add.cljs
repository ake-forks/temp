(ns darbylaw.web.ui.pensions.add
  (:require
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.shared :as shared]
    [darbylaw.web.ui.pensions.form :as form]
    [darbylaw.web.ui :as ui :refer (<<)]))

(rf/reg-event-fx ::add-success
  (fn [{:keys [db]} [_  case-id {:keys [path]}]]
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::model/hide-dialog]]
          [:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::add-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::add-pension
  (fn [{:keys [db]} [_ pension-type case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri
        (case pension-type
          :private (str "/api/case/" case-id "/pension/add-private")
          :state (str "/api/case/" case-id "/pension/add-state"))
        :params (if (= :private pension-type)
                  (assoc values :provider (keyword (:provider values)))
                  values)
        :on-success [::add-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))

(defn state-layout [{:keys [handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/dialog-content {:style shared/dialog-size}
    [shared/dialog-header "add a state pension"]
    [mui/stack {:spacing 2}
     [mui/typography {:variant :h6} "Tell Us Once"]
     [mui/divider]
     [form/tell-us-once-field fork-args]
     [mui/typography {:variant :body1} "If you have used the UK government's "
      [mui/link {:href "https://www.gov.uk/after-a-death/organisations-you-need-to-contact-and-tell-us-once"
                 :rel "noopener noreferrer"
                 :target "_blank"} "Tell Us Once"]
      " service, please enter your reference here."]
     [mui/typography {:variant :h6} "pension details"]
     [mui/divider]
     [form/ni-field fork-args]
     [form/ref-field fork-args]
     [form/start-date-field fork-args]]]
   [mui/dialog-actions
     [mui/button {:type :submit} "save"]]])

(defn private-layout [{:keys [handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/dialog-content {:style shared/dialog-size}
    [shared/dialog-header "add a private pension"]
    [mui/stack {:spacing 2}
     [mui/typography {:variant :h6} "pension details"]
     [mui/divider]
     [form/company-select fork-args]
     [form/ni-field fork-args]
     [form/ref-field fork-args]]]
   [mui/dialog-actions
    [mui/button {:type :submit} "save"]]])

(defn panel []
  (let [case-id (<< ::case-model/case-id)
        dialog (<< ::model/dialog)
        default-props {:open (or (:open dialog) false)
                       :maxWidth false
                       :scroll :paper}
        pension-type (:pension-type dialog)]
    (when (:open dialog)
      [mui/dialog default-props
       [form/form {:layout (case pension-type
                             :private private-layout
                             :state state-layout)
                   :submit-fn #(rf/dispatch [::add-pension pension-type case-id %])}]])))