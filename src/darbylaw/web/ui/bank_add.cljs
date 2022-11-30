(ns darbylaw.web.ui.bank-add
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [vlad.core :as v]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.bank-model :as bank-model]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.api.bank-list :as banks]
            [darbylaw.web.util.bank :as bank-utils])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(rf/reg-event-fx ::add-bank-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    (print response)
    {:db (fork/set-submitting db path false)}
    (rf/dispatch [::bank-model/hide-bank-dialog])))

(rf/reg-event-fx ::add-bank-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (print response)
             (fork/set-submitting db path false))}))

(defn transform-on-submit [data]
  (update data :bank-id keyword))

(rf/reg-event-fx ::add-bank
  (fn [{:keys [db]} [_ case-id bank-dialog {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/bank-api/" case-id (if (= bank-dialog :add-bank) "/add-bank-accounts" "/update-bank-accounts"))
        :params (transform-on-submit values)
        :on-success [::add-bank-success fork-params]
        :on-failure [::add-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id bank-dialog fork-params]]
    {:dispatch [::add-bank case-id bank-dialog fork-params]}))

(defn bank-label [bank-id-str]
  (assert (string? bank-id-str))
  (banks/bank-label (keyword bank-id-str)))

(defn bank-select [{:keys [values set-handle-change handle-blur touched errors] :as fork-args}]
  (let [bank-dialog @(rf/subscribe [::bank-model/bank-dialog])]
    [:<>
     [mui/autocomplete
      {:options (banks/all-bank-ids)
       :value (get values :bank-id)
       :disabled (not= bank-dialog :add-bank)
       :getOptionLabel bank-label
       :onChange (fn [_evt new-value]
                   (set-handle-change {:value new-value
                                       :path [:bank-id]}))
       :renderInput (react-component [props]
                      [mui/text-field (merge props
                                        {:name :bank-id
                                         :label "bank name"
                                         :required true
                                         :onBlur handle-blur
                                         :error (when (touched :bank-id) (get-in errors [:errors :bank-id :error?]))})])}]]))

(defn account-array-fn
  [{:keys [errors] :as props}
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur
                      touched]}]
  (let [bank-id @(rf/subscribe [::bank-model/bank-dialog])
        sub @(rf/subscribe [::bank-model/banks-complete])
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
                                :error (boolean (bank-utils/get-account-error errors touched :sort-code idx))
                                :helper-text (if
                                               (boolean (bank-utils/get-account-error errors touched :sort-code idx))
                                               "format 00-00-00")}]

               [mui/text-field {:name :account-number
                                :value (or (get field :account-number) "")
                                :label "account number"
                                :placeholder "00000000"
                                :on-change #(handle-change % idx)
                                :on-blur #(handle-blur % idx)
                                :required true
                                :full-width true
                                :error (boolean (bank-utils/get-account-error errors touched :account-number idx))
                                :helper-text (if
                                               (boolean (bank-utils/get-account-error errors touched :account-number idx))
                                               "8 digits")}]

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
                                         :control (r/as-element
                                                    [mui/checkbox {:name :joint-check
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
     (if (and (not (= bank-id :add-bank)) (not complete))
       [mui/button {:on-click #(rf/dispatch [::bank-model/mark-bank-complete bank-id])
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
   [mui/button {:onClick #(rf/dispatch [::bank-model/hide-bank-dialog])
                :variant :contained :full-width true} "cancel"]
   [mui/button {:type :submit :variant :contained :full-width true} "save"]])

(defn add-bank-panel [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        bank-dialog @(rf/subscribe [::bank-model/bank-dialog])]
    [:form {:on-submit handle-submit}
     [mui/box {:style {:background-color :white}}
      [mui/stack {:spacing 1 :style {:padding "1rem"}}

       (if (= bank-dialog :add-bank)
         [mui/typography {:variant :h5} "add bank accounts"]
         [mui/stack {:direction :row :spacing 1 :justify-content :space-between}
          [mui/typography {:variant :h5} "edit accounts"]])
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

(def account-validation
  (v/join
    (v/attr [:sort-code]
      (v/chain
        (v/present)
        (v/matches #"\d{2}-\d{2}-\d{2}")))
    (v/attr [:account-number]
      (v/chain
        (v/present)
        (v/matches #"[0-9]{8}")))))

(def id-validation
  (v/join
    (v/attr [:bank-id] (v/present))))

(defn validation [values]
  (merge (map (fn [acc]
                (v/field-errors account-validation acc))
           (:accounts values))
    (v/field-errors id-validation values)))

(defonce form-state (r/atom nil))

(defn dialog []
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
          bank-dialog @(rf/subscribe [::bank-model/bank-dialog])]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit #(rf/dispatch [::submit! case-id bank-dialog %])
        :keywordize-keys true
        :prevent-default? true
        :validation (fn [data]
                      (try
                        (validation data)
                        (catch :default e
                          (js/console.error "Error during validation: " e)
                          [{:type ::validation-error :error e}])))
        :initial-values {:accounts [{}]}}                   ; placeholder for entering first account
       (fn [fork-args]
         [add-bank-panel (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))

(defn dialog-with-values [values]
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
          bank-dialog @(rf/subscribe [::bank-model/bank-dialog])]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit #(rf/dispatch [::submit! case-id bank-dialog %])
        :keywordize-keys true
        :prevent-default? true
        :validation (fn [data]
                      (try
                        (validation data)
                        (catch :default e
                          (js/console.error "Error during validation: " e)
                          [{:type ::validation-error :error e}])))
        :initial-values values}
       (fn [fork-args]
         [add-bank-panel (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))


