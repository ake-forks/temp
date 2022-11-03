(ns darbylaw.web.ui.bank
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]

            [fork.re-frame :as fork]
            [darbylaw.web.ui :as ui]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [darbylaw.workspaces.workspace-icons :as icon]
            [vlad.core :as v])
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
    (:path-params (:kee-frame/route db))))

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
        :uri (str "/api/case/" case-id "/add-bank")
        :params {:bank-info values}
        :on-success [::add-bank-success fork-params]
        :on-failure [::add-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:dispatch [::add-bank case-id fork-params]}))

(defn generate-banks [banks]
  (mapv
    (fn [bank] assoc {} :id (:id bank) :label (:common-name bank)) banks))

(defn bank-select [{:keys [values set-handle-change handle-blur] :as fork-args} banks]
  [mui/autocomplete
   {:options (generate-banks banks)
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
  [{:keys [values errors] :as _props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur
                      touched]}]
  [mui/stack {:spacing 1}
   (doall
     (->> fields
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
                              :placeholder "00-00-00"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :full-width true
                              :helper-text (when (touched idx :sort-code))}]
             [mui/text-field {:name :account-number
                              :value (get field :account-number)
                              :label "account number"
                              :placeholder "00000000"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true}]
             [mui/text-field {:name :estimated-value
                              :value (get field :estimated-value)
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :full-width true
                              :InputProps
                              {:start-adornment
                               (r/as-element [mui/input-adornment {:position :start} "£"])}}]
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

              [:<>])]))))
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

(defn submit-buttons []
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between}
   [mui/button {:onClick #(rf/dispatch [::hide-bank-modal])
                :variant :contained :full-width true} "cancel"]
   [mui/button {:type :submit :variant :contained :full-width true} "save"]])

(defn modal-panel [{:keys [values handle-submit] :as fork-args} banks]
  (let [current-case @(rf/subscribe [::current-case])]
    [:form {:on-submit handle-submit}
     [mui/container {:style {:margin-top "4rem" :background-color :white}}
      [mui/stack {:spacing 1 :style {:padding "1rem"}}
       [mui/typography {:variant :h3} "add a bank"]
       [bank-select fork-args banks]
       [mui/typography {:variant :h6}
        (str "To the best of your knowledge, enter the details for all of your "
          (-> current-case :deceased :relationship (clojure.string/lower-case))
          "'s accounts")
        (if (str/blank? (get values :bank-name))
          "."
          (str " with " (get values :bank-name) "."))]
       [fork/field-array {:props fork-args
                          :name :account}
        account-array-fn]
       [submit-buttons]]]]))


;(def validation
;  (v/join
;    (v/attr [:account-number] (v/chain (v/matches #"[0-9]{8}")))))

(defonce form-state (r/atom nil))

(defn modal [banks]
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
       [modal-panel (ui/mui-fork-args fork-args) banks])]))

