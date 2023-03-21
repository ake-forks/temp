(ns darbylaw.web.ui.properties.model
  (:require
    [fork.re-frame :as fork]
    [medley.core :as medley]
    [re-frame.core :as rf]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :as ui :refer (<<)]))

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
    (assoc-in db [:dialog/property] {:open false})))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/property db)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    {:dispatch [::case-model/load-case! case-id]}))
(rf/reg-event-fx ::upload-failure
  (fn [_ [_ response]]
    {:dispatch [::reset-file-uploading]
     ::ui/notify-user-http-error {:message "Error uploading."
                                  :result response}}))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ case-id file]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/document")
        :body (doto (js/FormData.)
                (.append "file" file)
                (.append "filename" (.-name file))
                (.append "postcode" "sw14"))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

(rf/reg-event-fx ::add-success
  (fn [{:keys [db]} [_ case-id {:keys [path] :as _fork-params}]]
    (print "success")
    {:db (fork/set-submitting db path false)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::add-failure
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} error-result]]
    (print error-result)
    {:db (fork/set-submitting db path false)
     ::ui/notify-user-http-error {:message "add-failure error"
                                  :result error-result}}))


(rf/reg-event-fx ::add-property
  (fn [{:keys [db]} [_ case-id {:keys [path values] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/property/" case-id "/add-property")
        :body (ui/make-form-data (merge
                                   (-> values
                                     (select-keys [:address :valuation
                                                   :joint-ownership? :joint-owner])
                                     (update-vals pr-str))
                                   (dissoc values :file-count :address :valuation :joint-ownership? :joint-owner)))
        :timeout 16000
        :on-success [::add-success case-id fork-params]
        :on-failure [::add-failure fork-params]})}))

