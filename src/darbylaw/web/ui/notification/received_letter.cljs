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
            [vlad.core :as v]))

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

(defn letter-icon []
  [mui/badge {:badgeContent "\u2199"
              :color :primary
              :sx {"& .MuiBadge-badge" {:font-weight :bold
                                        :background-color :text.primary}}}
   [ui/icon-mail-outlined]])

(defn pdf-view-panel []
  [pdf-viewer {:src (let [letter (<< ::model/open-letter)]
                      (str "/api/case/" (:probate.received-letter/case letter)
                           "/received-letter/" (:xt/id letter) "/pdf"))
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
       [file-input-button {:button-props {:startIcon (r/as-element [ui/icon-description-outlined])
                                          :disableRipple true
                                          :disableFocusRipple true
                                          :sx {:height 1 :width 1}}
                           :accept http/pdf-mime-type
                           :on-file-selected #(set-values {:file %})
                           :on-load-data-url #(rf/dispatch [::set-file-data-url %])}
        "upload pdf scan..."]])))

(defn content [{:keys [values handle-change handle-submit submitting? errors]
                :as fork-args}]
  [mui/stack {:sx {:height 1}}
   [letter-header {:on-back close!}
    [mui/list-item
     [mui/list-item-icon {:sx {:color :unset}}
      [letter-icon]]
     [mui/list-item-text
      {:primary "new received letter"
       :secondary "in preparation"}]]]

   (case (<< ::edit-mode)
     :create [pdf-upload-panel fork-args]
     :update [pdf-view-panel])

   [mui/dialog-content {:sx {:flex-grow 0}}
    [mui/form-control-label
     {:control (r/as-element [mui/checkbox])
      :name :contains-valuation
      :checked (:contains-valuation values false)
      :on-change handle-change
      :label "Contains valuation information"}]]

   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :onClick close!}
     "Cancel"]
    [mui/button {:variant :contained
                 :onClick handle-submit
                 :disabled (boolean (or (seq errors)
                                        submitting?))}
     "Save"]]])

(rf/reg-fx ::close close!)

(rf/reg-event-fx ::create-received-letter-success
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} notification-data]]
    {:db (fork/set-submitting db path false)
     ::close nil
     :dispatch [::model/load-conversation notification-data]}))

(rf/reg-event-fx ::create-received-letter-failure
  (fn [{:keys [db]} [_ {:keys [path] :as _fork-params} error-result]]
    {:db (fork/set-submitting db path false)
     ::close nil
     ::ui/notify-user-http-error {:result error-result}}))

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
  (-> @re-frame.db/app-db :current-case :utility-bills first))
