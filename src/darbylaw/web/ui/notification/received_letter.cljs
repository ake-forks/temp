(ns darbylaw.web.ui.notification.received-letter
  (:require [darbylaw.api.util.http :as http]
            [darbylaw.web.ui :as ui :refer [<<]]
            [darbylaw.web.util.vlad :as v+]
            [re-frame.core :as rf]
            [re-frame.db]
            [fork.re-frame :as fork]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui.notification.model :as model]
            [darbylaw.web.ui.notification.letter-commons :refer [letter-header]]
            [reagent.core :as r]
            [darbylaw.web.ui.components.pdf-viewer :refer [pdf-viewer]]
            [darbylaw.web.ui.components.file-input-button :refer [file-input-button]]
            [darbylaw.web.util.form :as form-util]
            [vlad.core :as v]
            [darbylaw.web.ui.mailing.letter-commons :as letter-commons]
            [darbylaw.global :as global]))

(rf/reg-event-db ::set-file-data-url
  (fn [db [_ data-url]]
    (assoc-in db [::model/context :upload-received-letter :file-data-url] data-url)))

(rf/reg-sub ::file-data-url
  :<- [::model/context]
  (fn [context]
    (get-in context [:upload-received-letter :file-data-url])))

(rf/reg-sub ::edit-mode
  :<- [::model/open-letter]
  (fn [open-letter]
    (if open-letter :update :create)))

(defn close! []
  (case (<< ::edit-mode)
    :create (rf/dispatch [::model/open-upload-received-letter false])
    :update (rf/dispatch [::model/close-letter])))

(defn pdf-view-panel []
  [pdf-viewer {:src (str "/api/case/" (<< ::model/open-letter-case-id)
                         "/received-letter/" (<< ::model/open-letter-id) "/pdf")
               :sx {:flex-grow 1}}])

(defn pdf-upload-panel [{:keys [set-values] :as _fork-args}]
  (let [file-data-url (<< ::file-data-url)]
    (cond
      file-data-url
      [pdf-viewer {:src file-data-url
                   :sx {:flex-grow 1}}]

      :else
      [mui/card {:variant :outlined
                 :sx {:flex-grow 1
                      :m 1
                      :border-width 4
                      :border-style :dashed}}
       [file-input-button
        {:button-props {:startIcon (r/as-element [ui/icon-description-outlined])
                        :disableRipple true
                        :disableFocusRipple true
                        :sx {:height 1 :width 1}}
         :accept http/pdf-mime-type
         :on-selected (fn [f]
                        (if (> (.-size f) global/max-request-size)
                          (rf/dispatch [::ui/show-message {:severity :error
                                                           :text "File is too big."}])
                          (do (set-values {:file f})
                              (rf/dispatch [::set-file-data-url (js/URL.createObjectURL f)]))))}
        "upload pdf scan..."]])))

(defonce delete-confirmation-open? (r/atom false))

(defn delete-confirmation-dialog []
  [mui/dialog {:open (boolean @delete-confirmation-open?)
               :maxWidth :sm}
   [mui/dialog-title "confirm delete"]
   [mui/dialog-content
    "Do you want to delete the letter?"]
   [mui/dialog-actions
    [mui/button {:variant :contained
                 :onClick #(reset! delete-confirmation-open? false)}
     "No, cancel"]
    [mui/button {:variant :outlined
                 :color :error
                 :onClick (let [case-id (<< ::model/open-letter-case-id)
                                letter-id (<< ::model/open-letter-id)]
                            (fn []
                              (rf/dispatch
                                [::model/delete-letter
                                 {:case-id case-id
                                  :letter-id letter-id
                                  :on-completed
                                  #(reset! delete-confirmation-open? false)}])))}
     "Yes, delete"]]])

(defn content [{:keys [_values _handle-change handle-submit submitting? errors]
                :as fork-args}]
  [mui/stack {:sx {:height 1}}
   [letter-header {:on-back close!}
    [mui/list-item
     [mui/list-item-icon {:sx {:color :unset}}
      [letter-commons/icon-received-letter]]
     [mui/list-item-text
      {:primary "new received letter"
       :secondary "in preparation"}]]
    (when (= :update (<< ::edit-mode))
      [mui/stack {:direction :row
                  :sx {:align-self :center}}
       [delete-confirmation-dialog]
       [mui/button {:variant :outlined
                    :color :error
                    :startIcon (r/as-element [ui/icon-delete])
                    :onClick #(reset! delete-confirmation-open? true)}
        "Delete"]])]

   (case (<< ::edit-mode)
     :create [pdf-upload-panel fork-args]
     :update [pdf-view-panel])

   ; Removed for now.
   #_[mui/dialog-content {:sx {:flex-grow 0}}
      [mui/form-control-label
       {:control (r/as-element [mui/checkbox])
        :name :contains-valuation
        :checked (:contains-valuation values false)
        :on-change handle-change
        :label "Contains valuation information"}]]

   (when (= :create (<< ::edit-mode))
     [mui/dialog-actions
      [mui/button {:variant :outlined
                   :onClick close!}
       "Cancel"]
      [mui/button {:variant :contained
                   :onClick handle-submit
                   :disabled (boolean (or (seq errors)
                                          submitting?))}
       "Save"]])])

(rf/reg-fx ::close close!)

(rf/reg-event-fx ::create-received-letter-success
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} notification-data]]
    {:db (fork/set-submitting db path false)
     ::close nil
     :dispatch [::model/load-conversation notification-data]}))

(rf/reg-event-fx ::create-received-letter-failure
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} error-result]]
    {:db (fork/set-submitting db path false)
     ::ui/notify-user-http-error {:message "Could not create letter"
                                  :result error-result}}))

(rf/reg-event-fx ::create-received-letter
  (fn [{:keys [db]} [_
                     {:keys [case-id] :as notification-data}
                     {:keys [values path] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/received-letter")
        :body (ui/make-form-data (merge (-> notification-data
                                          (dissoc :case-id)
                                          (update-vals pr-str))
                                        (-> values
                                          (dissoc :file)
                                          (update-vals pr-str))
                                        (select-keys values [:file])))
        :timeout 16000
        :on-success [::create-received-letter-success fork-params notification-data]
        :on-failure [::create-received-letter-failure fork-params]})}))

(defonce form-state (r/atom nil))

(def data-validation
  (v/attr [:file] (v+/v-some?)))

(defn panel []
  (let [notification-data (<< ::model/notification)]
    [form-util/form
     {:state form-state
      :path [::form]
      :on-submit (fn [fork-params]
                   (rf/dispatch [::create-received-letter notification-data fork-params]))
      :validation (fn [data]
                    (v/field-errors data-validation data))}
     (fn [fork-args]
       [content fork-args])]))

(comment
  (pr-str (-> @re-frame.db/app-db :current-case :id))
  (-> @re-frame.db/app-db :current-case :utilities first))
