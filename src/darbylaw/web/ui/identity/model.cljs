(ns darbylaw.web.ui.identity.model
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.api.smart-search.data :as ss-data]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent.core :as r]))


;; >> Dialog

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::dialog-open? true
                 ::dialog-context dialog-context})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (::dialog-open? db)))

(rf/reg-sub ::submitting?
  (fn [db]
    (::submitting? db)))



;; >> Data

(rf/reg-sub ::uk-aml
  :<- [::case-model/current-case]
  #(when-let [uk-aml (:uk-aml %)]
     (assoc uk-aml :final-result (ss-data/uk-aml->result uk-aml))))

(rf/reg-sub ::fraudcheck
  :<- [::case-model/current-case]
  #(when-let [fraudcheck (:fraudcheck %)]
     (assoc fraudcheck :final-result (ss-data/fraudcheck->result fraudcheck))))

(rf/reg-sub ::smartdoc
  :<- [::case-model/current-case]
  #(when-let [smartdoc (:smartdoc %)]
     (assoc smartdoc :final-result (ss-data/smartdoc->result smartdoc))))

(rf/reg-sub ::has-checks?
  :<- [::uk-aml]
  :<- [::fraudcheck]
  :<- [::smartdoc]
  (fn [[uk-aml fraudcheck smartdoc] _]
    (or (seq uk-aml) (seq fraudcheck) (seq smartdoc))))

(rf/reg-sub ::override-result
  :<- [::case-model/current-case]
  #(:override-identity-check %))

(rf/reg-sub ::current-final-result
  :<- [::override-result]
  :<- [::has-checks?]
  :<- [::uk-aml]
  :<- [::fraudcheck]
  :<- [::smartdoc]
  (fn [[override-result has-checks? uk-aml fraudcheck smartdoc]]
    (if override-result
      override-result
      (if-not has-checks?
        :unknown
        (if-not (= #{:pass}
                   (->> [uk-aml fraudcheck smartdoc]
                        (map :final-result)
                        (into #{})))
          :fail
          (if (= :processing (:final-result smartdoc))
            :processing
            :pass))))))

(rf/reg-sub ::note
  :<- [::case-model/current-case]
  #(get-in % [:identity-check-note :note]))

(rf/reg-sub ::user-documents
  :<- [::case-model/current-case]
  #(:identity-user-docs %))



;; >> Generic Submit Effects

(rf/reg-event-fx ::submit-success
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::submitting? false)
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::submit-failure
  (fn [{:keys [db]} [_ message error-result]]
    {:db (assoc db ::submitting? false)
     ::ui/notify-user-http-error {:message message
                                  :result error-result}}))



;; >> Identity Submit Effects

(rf/reg-event-fx ::identity-check
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::submitting? true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity/checks/run")
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error starting identity checks"]})}))



;; >> Override Submit Effects

(rf/reg-event-fx ::set-override-result
  (fn [_ [_ case-id new-result]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity/checks/override")
        :url-params (when new-result
                      {:new-result (name new-result)})
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error overriding result"]})}))



;; >> Note Submit Effects

(rf/reg-event-fx ::note-submit-success
  (fn [{:keys [db]} [_ case-id]]
    {:db (assoc db ::note-submit-success true)
     :dispatch [::submit-success case-id]
     :dispatch-later {:ms 1000 :dispatch [::note-clear-status]}}))

(rf/reg-event-fx ::note-submit-failure
  (fn [{:keys [db]} [_ message error-result]]
    {:db (assoc db ::note-submit-success false)
     :dispatch-later {:ms 1000 :dispatch [::note-clear-status]}}))
 
(rf/reg-event-db ::note-clear-status
  (fn [db _]
    (dissoc db ::note-submit-success)))

(rf/reg-sub ::note-submit-status
  (fn [db]
    (::note-submit-success db)))

(rf/reg-event-fx ::save-note
  (fn [{:keys [db]} [_ case-id values]]
    {:db (dissoc db ::note-submit-success)
     :http-xhrio
     (ui/build-http
       {:method :post
        :timeout 10000
        :uri (str "/api/case/" case-id "/identity/note")
        :params values
        :on-success [::note-submit-success case-id]
        :on-failure [::note-submit-failure "Error saving note"]})}))



;; >> Document Upload Submit Effects

(rf/reg-event-fx ::upload-document
  (fn [_ [_ case-id document]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/identity/document")
        :body (let [form-data (js/FormData.)]
                (.append form-data "file" document)
                form-data)
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error uploading user document"]})}))

(rf/reg-event-fx ::delete-document
  (fn [_ [_ case-id document-id]]
    {:http-xhrio
     (ui/build-http
       {:method :delete
        :uri (str "/api/case/" case-id "/identity/document/" document-id)
        :on-success [::submit-success case-id]
        :on-failure [::submit-failure "Error deleting user document"]})}))
