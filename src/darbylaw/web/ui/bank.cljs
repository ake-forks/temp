(ns darbylaw.web.ui.bank
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [darbylaw.web.ui.components :as c]
            [darbylaw.web.routes :as routes]
            [fork.re-frame :as fork]
            [darbylaw.web.ui :as ui]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [darbylaw.workspaces.workspace-icons :as icon])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(rf/reg-event-db
  ::show-bank-modal
  (fn [db _]
    (assoc-in db [:modal/bank-modal] true)))

(rf/reg-event-db
  ::hide-bank-modal
  (fn [db _]
    (assoc-in db [:modal/bank-modal] false)))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))

(rf/reg-sub ::current-case
  (fn [db _]
    (:current-case db)))

(rf/reg-sub ::route-params
  (fn [db _]
    (:route-params db)))

(rf/reg-event-fx ::add-bank-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}
    (rf/dispatch [::hide-bank-modal])))

(rf/reg-event-fx ::add-bank-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::add-bank
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :patch
        :uri (str "/api/bank/" case-id)
        :params {:bank-info values}
        :on-success [::add-bank-success fork-params]
        :on-failure [::add-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:dispatch [::add-bank case-id fork-params]}))


(defn bank-select [{:keys [values set-handle-change handle-blur] :as fork-args}]
  [mui/autocomplete
   {:options c/list-of-banks
    :inputValue (or (get values :bank-name) "")
    :onInputChange (fn [_evt new-value]
                     (set-handle-change {:value new-value
                                         :path [:bank-name]}))
    :renderInput (react-component [props]
                   [mui/text-field (merge props
                                     {:name :bank-name
                                      :label "Bank Name"
                                      :required true
                                      :onBlur handle-blur})])}])

(defn account-array-fn
  [_props
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur]}]
  [mui/stack {:spacing 1}
   (map-indexed
     (fn [idx field]
       ^{:key idx}
       [:<>
        [mui/stack {:spacing 1 :direction :row}
         (if (> idx 0)
           [mui/icon-button {:on-click #(when (> (count fields) 1) (remove idx))}
            [ui/icon-delete]] [:<>])
         [mui/text-field {:name :sort-code
                          :value (get field :sort-code)
                          :label "sort code"
                          :on-change #(handle-change % idx)
                          :on-blur #(handle-blur % idx)
                          :required true
                          :full-width true}]
         [mui/text-field {:name :account-number
                          :value (get field :account-number)
                          :label "account number"
                          :on-change #(handle-change % idx)
                          :on-blur #(handle-blur % idx)
                          :required true
                          :full-width true}]
         [mui/text-field {:name :estimated-value
                          :value (get field :estimated-value)
                          :label "estimated value"
                          :on-change #(handle-change % idx)
                          :on-blur #(handle-blur % idx)
                          :full-width true}]
         [mui/form-group
          [mui/form-control-label {
                                   :control (r/as-element [mui/checkbox {:name :joint-check
                                                                         :value (get field :joint-check)
                                                                         :label "estimated value"
                                                                         :on-change #(handle-change % idx)}])
                                   :label "Joint Account?"}]]]

        (if (true? (get field :joint-check))
          [mui/text-field {:name :joint-info
                           :value (get field :joint-info)
                           :label "name of other account holder"
                           :on-change #(handle-change % idx)}]

          [:<>])])

     fields)
   [mui/button {:on-click #(insert {:sort-code "" :account-number "" :estimated-value ""})
                :style {:text-transform "none" :align-self "baseline" :font-size "1.5rem"}
                :variant :text
                :size "large"
                :full-width false
                :start-icon (r/as-element [icon/mui-add])}
    (str "add another "
      (if (str/blank? (get (:values _props) :bank-name))
        "account"
        (str (get (:values _props) :bank-name) " account")))]])

(defn submit-buttons [fork-args case-id]
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between}
   [mui/button {:onClick #(rf/dispatch [::hide-bank-modal]) :variant :contained :full-width true} "cancel"]
   [mui/button {:type :submit :variant :contained :full-width true} "save"]])

#_(defn bank-panel [{:keys [values handle-submit] :as fork-args}]
    (let [current-case @(rf/subscribe [::current-case])
          case-id (-> @(rf/subscribe [::route-params]) :case-id)]

      [:form {:on-submit handle-submit}
       [mui/container {:style {:margin-top "4rem"}}
        [mui/stack {:spacing 1}
         [mui/typography {:variant :h3} "add a bank"]
         [bank-select fork-args]
         [mui/typography {:variant :h6}
          (str "To the best of your knowledge, enter the details for all of your "
            (-> current-case :deceased :relationship) "'s accounts")
          (if (str/blank? (get values :bank-name)) "." (str " with " (get values :bank-name) "."))]
         [fork/field-array {:props fork-args
                            :name :account}
          account-array-fn]]]]))


(defn modal-panel [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::current-case])
        case-id (-> @(rf/subscribe [::route-params]) :case-id)]

    [:form {:on-submit handle-submit}
     [mui/container {:style {:margin-top "4rem" :background-color :white}}
      [mui/stack {:spacing 1 :style {:padding "1rem"}}
       [mui/typography {:variant :h3} "add a bank"]
       [bank-select fork-args]
       [mui/typography {:variant :h6}
        (str "To the best of your knowledge, enter the details for all of your "
          (-> current-case :deceased :relationship) "'s accounts")
        (if (str/blank? (get values :bank-name)) "." (str " with " (get values :bank-name) "."))]
       [fork/field-array {:props fork-args
                          :name :account}
        account-array-fn]
       [submit-buttons]]]]))


(defonce form-state (r/atom nil))

(defn modal []
  (let [case-id (-> @(rf/subscribe [::route-params]) :case-id)]
    [fork/form
     {
      :state form-state
      :clean-on-unmount? true
      :on-submit #(rf/dispatch [::submit! case-id %])
      :keywordize-keys true
      :prevent-default? true
      :initial-values {:bank-name "" :account [{:sort-code "" :account-number "" :estimated-value ""}]}}
     (fn [fork-args]
       [modal-panel (ui/mui-fork-args fork-args)])]))


#_(defn panel []
    (let [case-id (-> @(rf/subscribe [::route-params]) :case-id)]
      [fork/form
       {
        :state form-state
        :clean-on-unmount? true
        :on-submit #(rf/dispatch [::submit! case-id %])
        :keywordize-keys true
        :prevent-default? true
        :initial-values {:bank-name "" :account [{:sort-code "" :account-number "" :estimated-value ""}]}}
       (fn [fork-args]
         [:<>
          [c/navbar]

          [bank-panel (ui/mui-fork-args fork-args)]
          [c/footer]])]))

#_(defmethod routes/panels :bank-panel [] [panel])

