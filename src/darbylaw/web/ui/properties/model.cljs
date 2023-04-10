(ns darbylaw.web.ui.properties.model
  (:require
    [clojure.string :as string]
    [fork.re-frame :as fork]
    [medley.core :as medley]
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :as ui :refer (<<)]
    [reagent.core :as r]))

(def edit-mode (r/atom false))

(def file-uploading? (r/atom false))

(def popover (r/atom {}))

(defn get-property [id]
  (let [all-props (<< ::case-model/properties)]
    (get (medley/index-by :id all-props) (uuid id))))

(rf/reg-event-db
  ::show-dialog
  (fn [db [_ id dialog-type]]
    (assoc-in db [:dialog/property]
      {:open true
       :id (str id)
       :dialog-type dialog-type})))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (reset! edit-mode false)
    (assoc-in db [:dialog/property] {:open false})))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/property db)))

(ui/reg-fx+event ::reset-file-uploading
  (fn [_]
    (reset! file-uploading? false)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    (reset! popover {})
    {:dispatch [::case-model/load-case! case-id]}))
(rf/reg-event-fx ::upload-failure
  (fn [_ [_ response]]
    (reset! popover {})
    {:dispatch [::reset-file-uploading]
     ::ui/notify-user-http-error {:message "Error: "
                                  :result response}}))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ case-id property-id file]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/post-document/" property-id)
        :body (doto (js/FormData.)
                (.append "file" file))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

(rf/reg-event-fx
  ::remove-file
  (fn [_ [_ case-id property-id filename]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/remove-document/" filename)
        :params {:property-id property-id}
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))
(rf/reg-event-fx ::open-document
  (fn [_ [_ case-id filename]]
    (js/window.open
      (str "/api/property/" case-id "/get-document/" filename))))

(rf/reg-event-fx ::add-success
  (fn [{:keys [db]} [_ case-id {:keys [path] :as _fork-params}]]
    (print "success")
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::hide-dialog]]]}))

(rf/reg-event-fx ::add-failure
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} error-result]]
    (print error-result)
    {:db (fork/set-submitting db path false)
     ::ui/notify-user-http-error {:message "add-failure error"
                                  :result error-result}}))

(rf/reg-event-fx ::edit-success
  (fn [{:keys [db]} [_ case-id {:keys [path] :as _fork-params}]]
    (reset! edit-mode false)
    {:db (fork/set-submitting db path false)
     :fx [[:dispatch [::case-model/load-case! case-id]]]}))

(rf/reg-event-fx ::edit-property
  (fn [{:keys [db]} [_ case-id property-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/edit-property/" property-id)
        :params (-> values
                  (select-keys [:address :valuation
                                :insured? :estimated-value?
                                :joint-ownership? :joint-owner])
                  (assoc :address (string/trim (:address values))))
        :timeout 16000
        :on-success [::edit-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))

(rf/reg-event-fx ::remove-success
  (fn [_ [_ case-id]]
    (reset! edit-mode false)
    (reset! popover {})
    {:fx [[:dispatch [::case-model/load-case! case-id]]
          [:dispatch [::hide-dialog]]]}))

(rf/reg-event-fx ::remove-failure
  (fn [_ [_ error-result]]
    (reset! popover {})
    {::ui/notify-user-http-error {:message "remove error"
                                  :result error-result}}))

(rf/reg-event-fx
  ::remove-property
  (fn [_ [_ case-id property-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/remove-property/" property-id)
        :on-success [::remove-success case-id]
        :on-failure [::remove-failure]})}))

(rf/reg-event-fx
  ::remove-owned
  (fn [_ [_ case-id property-id]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/remove-owned/" property-id)
        :on-success [::remove-success case-id]
        :on-failure [::remove-failure]})}))

(def non-file-fields
  [:file-count :address :valuation :joint-ownership? :joint-owner :insured? :estimated-value? :property])

(defn handle-address-value [values]
  (if (= (:property values) :new-property)
    values
    (assoc values :address (->
                             (filter #(= (:id %) (:property values)) (<< ::case-model/properties))
                             (first)
                             (:address)))))

(rf/reg-event-fx ::add-property
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    (let [new-property (= (:property values) :new-property)]
      {:db (fork/set-submitting db path true)
       :http-xhrio
       (ui/build-http
         {:method :post
          :uri (if new-property
                 (str "/api/property/" case-id "/add-property")
                 (str "/api/property/" case-id "/update-property/" (:property values)))
          :body (ui/make-form-data (merge
                                     (-> values
                                       (handle-address-value)
                                       (select-keys [:address :valuation
                                                     :insured? :estimated-value?
                                                     :joint-ownership? :joint-owner])
                                       (update-vals pr-str))
                                     (apply dissoc values non-file-fields)))
          :timeout 16000
          :on-success [::add-success case-id fork-params]
          :on-failure [::add-failure fork-params]})})))