(ns darbylaw.web.ui.keydocs.model
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent.core :as r]))

(rf/reg-event-db
  ::show-dialog
  (fn [db]
    (assoc-in db [:dialog/key-docs]
      true)))

(rf/reg-event-db
  ::hide-dialog
  (fn [db]
    (assoc-in db [:dialog/key-docs]
      false)))

(rf/reg-sub ::dialog
  (fn [db]
    (:dialog/key-docs db)))

(rf/reg-sub ::document-present?
  :<- [::case-model/current-case]
  (fn [current-case [_ document-name]]
    (contains? current-case document-name)))

(def file-uploading? (r/atom false))

(ui/reg-fx+event ::reset-file-uploading
  (fn [_]
    (reset! file-uploading? false)))

(rf/reg-event-fx ::upload-success
  (fn [_ [_ case-id]]
    {:dispatch [::case-model/load-case! case-id
                {:on-success [::reset-file-uploading]}]}))
(rf/reg-event-fx ::upload-failure
  (fn [_ [_ response]]
    {:dispatch [::reset-file-uploading]
     ::ui/notify-user-http-error {:message "Error uploading."
                                  :result response}}))

(rf/reg-event-fx ::upload-file
  (fn [_ [_ case-id file document-name]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/document/" (name document-name))
        :body (doto (js/FormData.)
                (.append "file" file)
                (.append "filename" (.-name file)))
        :format nil
        :on-success [::upload-success case-id]
        :on-failure [::upload-failure]})}))

(rf/reg-event-fx ::open-document
  (fn [_ [_ case-id document-name]]
    (js/window.open
      (str "/api/case/" case-id "/document/" (name document-name)))))




