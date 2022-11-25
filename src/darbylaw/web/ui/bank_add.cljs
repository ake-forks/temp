(ns darbylaw.web.ui.bank-add
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [fork.re-frame :as fork]
            [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]
            [darbylaw.workspaces.workspace-icons :as icon]
            [vlad.core :as v]
            [darbylaw.api.bank-list :as banks])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(rf/reg-event-db
  ::show-bank-modal
  (fn [db [_ value]]
    (assoc-in db [:modal/bank-modal] value)))

(rf/reg-event-db
  ::hide-bank-modal
  (fn [db _]
    (assoc-in db [:modal/bank-modal] nil)))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))

(rf/reg-event-db
  ::mark-bank-complete
  (fn [db [_ bank-id]]
    (update-in db [:banks-complete] conj bank-id)))

(rf/reg-sub ::banks-complete
  (fn [db _]
    (:banks-complete db)))

(rf/reg-sub ::current-case
  (fn [db _]
    (:current-case db)))

(rf/reg-sub ::route-params
  (fn [db _]
    (:path-params (:kee-frame/route db))))

(rf/reg-sub ::route
  (fn [db _]
    (:name (:data (:kee-frame/route db)))))

(rf/reg-event-fx ::add-bank-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (print response)
    {:db (fork/set-submitting db path false)}
    (rf/dispatch [::hide-bank-modal])))

(rf/reg-event-fx ::add-bank-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (print response)
             (fork/set-submitting db path false))}))

(defn transform-on-submit [data]
  (update data :bank-id keyword))


(rf/reg-event-fx ::add-bank
  (fn [{:keys [db]} [_ case-id modal-value {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/bank-api/" case-id (if (= modal-value :add-bank) "/add-bank-accounts" "/update-bank-accounts"))
        :params (transform-on-submit values)
        :on-success [::add-bank-success fork-params]
        :on-failure [::add-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id modal-value fork-params]]
    {:dispatch [::add-bank case-id modal-value fork-params]}))

(defn bank-label [bank-id-str]
  (assert (string? bank-id-str))
  (banks/bank-label (keyword bank-id-str)))

(defn bank-select [{:keys [values set-handle-change handle-blur] :as fork-args}]
  (let [bank-modal @(rf/subscribe [::bank-modal])]
    [mui/autocomplete
     {:options (banks/all-bank-ids)
      :value (get values :bank-id)
      :disabled (not= bank-modal :add-bank)
      :getOptionLabel bank-label
      :onChange (fn [_evt new-value]
                  (set-handle-change {:value new-value
                                      :path [:bank-id]}))
      :renderInput (react-component [props]
                     [mui/text-field (merge props
                                       {:name :bank-id
                                        :label "bank name"
                                        :required true
                                        :onBlur handle-blur})])}]))

(defn account-array-fn
  [{:keys [values errors] :as props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur
                      touched]}]
  (let [bank-id @(rf/subscribe [::bank-modal])
        sub @(rf/subscribe [::banks-complete])
        complete (some #(= bank-id %) sub)]
    [mui/stack {:spacing 1}
     (doall
       (->> fields
         (map-indexed
           (fn [idx field]
             ^{:key idx}
             [mui/stack (if (not (nil? complete)) {:spacing 1 :style {:margin-bottom "1rem"}}
                                                  {:spacing 1 :style {:margin-bottom 0}})
              [mui/stack {:spacing 1 :direction :row}

               [mui/text-field {:name :sort-code
                                :value (or (get field :sort-code) "")
                                :label "sort code"
                                :placeholder "00-00-00"
                                :on-change #(handle-change % idx)
                                :on-blur #(handle-blur % idx)
                                :required true
                                :full-width true
                                :helper-text (when (touched idx :sort-code))}]
               [mui/text-field {:name :account-number
                                :value (or (get field :account-number) "")
                                :label "account number"
                                :placeholder "00000000"
                                :on-change #(handle-change % idx)
                                :on-blur #(handle-blur % idx)
                                :required true
                                :full-width true}]
               [mui/text-field {:name :estimated-value
                                :value (or (get field :estimated-value) "")
                                :label "estimated value"
                                :on-change #(handle-change % idx)
                                :on-blur #(handle-blur % idx)
                                :full-width true
                                :disabled (if (not (nil? complete)) true false)
                                :InputProps
                                {:start-adornment
                                 (r/as-element [mui/input-adornment {:position :start} "£"])}}]


               [mui/form-group
                [mui/form-control-label {
                                         :control (r/as-element [mui/checkbox {:name :joint-check
                                                                               :value (get field :joint-check)
                                                                               :checked (get field :joint-check)
                                                                               :label "estimated value"
                                                                               :on-change #(handle-change % idx)}])
                                         :label "Joint Account?"}]]

               [mui/icon-button {:on-click #(remove idx)}
                [ui/icon-delete]]]

              (if (true? (get field :joint-check))
                [mui/text-field {:name :joint-info
                                 :value (if (true? (get field :joint-check)) (get field :joint-info) "")
                                 :label "name of other account holder"
                                 :on-change #(handle-change % idx)}]

                [:<>])
              (if (not (nil? complete))
                [mui/text-field {:name :confirmed-value
                                 :value (or (get field :confirmed-value) "")
                                 :label "confirmed value"
                                 :on-change #(handle-change % idx)
                                 :on-blur #(handle-blur % idx)
                                 :required true
                                 :full-width true
                                 :InputProps
                                 {:start-adornment
                                  (r/as-element [mui/input-adornment {:position :start} "£"])}}])]))))
     [mui/button {:on-click #(insert {:sort-code "" :account-number "" :estimated-value ""})
                  :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                  :variant :text
                  :size "large"
                  :full-width false
                  :start-icon (r/as-element [ui/icon-add-circle])}
      (str "add another "
        (if-let [bank-id (get-in props [:values :bank-id])]
          (str (bank-label bank-id) " account")
          "account"))]
     (if (not (= bank-id :add-bank))
       [mui/button {:on-click #(rf/dispatch [::mark-bank-complete bank-id])
                    :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                    :variant :text
                    :size "large"
                    :full-width false
                    :start-icon (r/as-element [ui/icon-check])}
        "mark accounts complete"])]))

(defn submit-buttons []
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between}
   [mui/button {:onClick #(rf/dispatch [::hide-bank-modal])
                :variant :contained :full-width true} "cancel"]
   [mui/button {:type :submit :variant :contained :full-width true} "save"]])

(defn modal-panel [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::current-case])
        bank-modal @(rf/subscribe [::bank-modal])]
    [:form {:on-submit handle-submit}
     [mui/box {:style {:background-color :white}}
      [mui/stack {:spacing 1 :style {:padding "1rem"}}
       (if (= bank-modal :add-bank)
         [mui/typography {:variant :h5} "add bank accounts"]
         [mui/stack {:direction :row :spacing 1 :justify-content :space-between}
          [mui/typography {:variant :h5} "edit accounts"]
          [mui/button {:on-click #(rf/dispatch [::ui/navigate [:bank-confirmation
                                                               {:case-id (:id current-case)
                                                                :bank-id bank-modal}]])}
           [mui/typography {:variant :body1}
            "view bank"]]])
       [bank-select fork-args]
       [mui/typography {:variant :h6}
        (if (some? (-> current-case :deceased :relationship))
          (str "To the best of your knowledge, enter the details for all of your "
            (-> current-case :deceased :relationship)
            "'s accounts")
          "To the best of your knowledge, enter the details for all of the deceased's accounts")
        (when-let [bank-id (:bank-id values)]
          (str " with " (bank-label bank-id)))
        "."]
       [fork/field-array {:props fork-args
                          :name :accounts}
        account-array-fn]
       [submit-buttons]]]]))



;(def validation
;  (v/join
;    (v/attr [:account-number] (v/chain (v/matches #"[0-9]{8}")))))

(defonce form-state (r/atom nil))

(defn modal []
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::route-params]) :case-id)
          modal-value @(rf/subscribe [::bank-modal])]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit #(rf/dispatch [::submit! case-id modal-value %])
        :keywordize-keys true
        :prevent-default? true
        :initial-values {:accounts [{}]}}                   ; placeholder for entering first account
       (fn [fork-args]
         [modal-panel (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))

(defn modal-with-values [values]
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::route-params]) :case-id)
          modal-value @(rf/subscribe [::bank-modal])]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit #(rf/dispatch [::submit! case-id modal-value %])
        :keywordize-keys true
        :prevent-default? true
        :initial-values values}
       (fn [fork-args]
         [modal-panel (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))


