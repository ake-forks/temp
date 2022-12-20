(ns darbylaw.web.ui.bank-confirmation-view
  (:require [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui.bank-model :as bank-model]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-add :as bank-add]
            [darbylaw.api.bank-list :as bank-list]
            [fork.re-frame :as fork]
            [reagent.core :as r]
            [darbylaw.web.theme :as theme]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.bank-add :as bank]
            [vlad.core :as v]))

;utils for uploading and viewing valuation PDF
(def uploading? (r/atom false))

(rf/reg-sub ::values-uploaded?
  :<- [::bank-model/current-bank-data]
  (fn [bank-data]
    (some? (:valuation-letter-uploaded bank-data))))

(rf/reg-sub ::bank-id
  (fn [db]
    (:modal/bank-dialog db)))

(rf/reg-event-fx ::load-case-success
  (fn [_ _]
    (reset! uploading? false)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id bank-id]]
    {:fx [[:dispatch [::case-model/load-case! case-id
                      {:on-success [::load-case-success]}]]]}))

(rf/reg-event-fx ::upload-failure
  (fn [_ _]
    (reset! uploading? false)))

(rf/reg-event-fx ::upload
  (fn [_ [_ case-id bank-id file]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/bank/" (name bank-id) "/valuation-pdf")
        :body (doto (js/FormData.)
                (.append "file" file))
        :format nil
        :on-success [::upload-success case-id bank-id]
        :on-failure [::upload-failure]})}))

(defn upload-button [_case-id _bank-id _props _label]
  (r/with-let [_ (reset! uploading? false)
               filename (r/atom "")]
    (fn [case-id bank-id props label]
      [ui/loading-button (merge props {:component "label"
                                       :loading @uploading?})
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::upload case-id bank-id selected-file])
                                (reset! filename "")
                                (reset! uploading? true))
                   :hidden true
                   :sx {:display :none}}]])))


;utils for editing bank account information
(rf/reg-event-fx ::update-bank-success
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}
    (rf/dispatch [::bank-model/hide-bank-dialog])))

(rf/reg-event-fx ::update-bank-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (do (assoc db :failure response)
             (fork/set-submitting db path false))}))

(rf/reg-event-fx ::update-bank
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/bank/" case-id "/update-bank-accounts")
        :params (bank/transform-on-submit values)
        :on-success [::update-bank-success fork-params]
        :on-failure [::update-bank-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id bank-id fork-params]]
    {:fx [[:dispatch [::update-bank case-id fork-params]]
          [:dispatch [::bank-model/mark-values-confirmed case-id bank-id]]]}))

(defn pdf-viewer [case-id bank-id]
  (if (false? @uploading?)
    [mui/stack {:spacing 1
                :style {:width "50%" :height "100%"}}
     [:iframe {:src (str "/api/case/" case-id "/bank/" (name bank-id) "/valuation-pdf")
               :width "100%"
               :height "100%"}]
     [upload-button case-id bank-id {:variant :contained} "replace pdf"]]
    [ui/loading-button {:loading true} "loading"]))


;pdf view and upload components
(defn upload-valuation-pdf []
  (let [values-uploaded? @(rf/subscribe [::values-uploaded?])
        case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])]
    (if values-uploaded?
      (if (false? @uploading?)
        [mui/stack {:spacing 1
                    :style {:width "100%" :height "100%"}}
         [:iframe {:src (str "/api/case/" case-id "/bank/" (name bank-id) "/valuation-pdf")
                   :width "100%"
                   :height "100%"}]
         [upload-button case-id bank-id {:variant :contained} "replace pdf"]]
        [ui/loading-button {:loading true :full-width true} "loading"])
      [mui/stack {:spacing 1}
       [mui/typography {:variant :body1}
        (str "Once you have received a letter of valuation from "
          (bank-list/bank-label bank-id) ", upload a PDF below.")]
       [upload-button case-id bank-id {:variant :contained} "upload pdf"]])))

;bank edit components
(defn bank-confirmation-form [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/stack {:spacing 2}
    [fork/field-array {:props fork-args
                       :name :accounts}
     bank-add/account-array-fn]]
   [mui/stack {:direction :row :spacing 1}
    [mui/button {:on-click #(rf/dispatch [::bank-model/hide-bank-dialog]) :variant :contained :full-width true} "cancel"]
    [mui/button {:type :submit :variant :contained :full-width true} "submit"]]])

(defonce form-state (r/atom nil))

(def conf-value-validation
  (v/attr [:confirmed-value]
    (v/chain
      (v/present)
      (v/matches #"[0-9]*(\.[0-9]{2})?"))))

(defn validation [values]
  (merge (map (fn [acc]
                (merge (v/field-errors bank-add/account-validation acc)
                  (v/field-errors conf-value-validation acc)))
           (:accounts values))
    {}))

;panel
(defn bank-confirmation-panel []
  (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        current-case @(rf/subscribe [::case-model/current-case])
        banks (-> @(rf/subscribe [::case-model/current-case]) :bank-accounts)
        current-bank (filter #(= (:id %) bank-id) banks)
        accounts (:accounts (first current-bank))]
    (rf/dispatch [::case-model/load-case! case-id])
    [mui/box
     [mui/stack {:style {:min-height "inherit"}
                 :spacing 1 :direction :row
                 :justify-content :space-between
                 :divider (r/as-element [mui/divider {:style {:border-color theme/rich-black
                                                              :border-width "1px"}}])}
      [mui/box
       [mui/stack {:spacing 1}
        [mui/typography {:variant :h5}
         (str "confirm your "
           (-> current-case :deceased :relationship)
           "'s accounts with " (bank-list/bank-label bank-id))]
        [mui/typography {:variant :p} (str "We have received a letter of valuation from "
                                        (bank-list/bank-label bank-id) ". Please enter the confirmed value for each of the accounts listed,
                                        and add any additional accounts mentioned in the letter.")]
        (if (some? accounts)
          (r/with-let []
            [fork/form
             {:state form-state
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit! case-id bank-id %])
              :keywordize-keys true
              :prevent-default? true
              :disable :estimated-value
              :initial-values {:accounts accounts :bank-id (name bank-id)}
              :validation (fn [data]
                            (try
                              (validation data)
                              (catch :default e
                                (js/console.error "Error during validation: " e)
                                [{:type ::validation-error :error e}])))}
             (fn [fork-args]
               [mui/box
                [bank-confirmation-form (ui/mui-fork-args fork-args)]])]
            (finally
              (reset! form-state nil))))]]]]))

