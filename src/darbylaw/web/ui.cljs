(ns darbylaw.web.ui
  (:require
    [reagent-mui.icons.account-balance]
    [reagent-mui.icons.add]
    [reagent-mui.icons.admin-panel-settings-outlined]
    [reagent-mui.icons.directions-run]
    [reagent-mui.icons.playlist-play]
    [reagent-mui.icons.pending]
    [reagent-mui.icons.arrow-back-sharp]
    [reagent-mui.icons.help-outline]
    [reagent-mui.icons.cloud-sync]
    [reagent-mui.icons.description-outlined]
    [reagent-mui.icons.download]
    [reagent-mui.icons.drafts-outlined]
    [reagent-mui.icons.error-outline]
    [reagent-mui.icons.edit]
    [reagent-mui.icons.expand-more]
    [reagent-mui.icons.history]
    [reagent-mui.icons.history-edu]
    [reagent-mui.icons.keyboard-arrow-up]
    [reagent-mui.icons.keyboard-arrow-down]
    [reagent-mui.icons.launch]
    [reagent-mui.icons.mail]
    [reagent-mui.icons.mail-outlined]
    [reagent-mui.icons.open-in-new]
    [reagent-mui.icons.link]
    [reagent-mui.icons.outbox]
    [reagent-mui.icons.priority-high]
    [reagent-mui.icons.add-circle]
    [reagent-mui.icons.person-outline]
    [reagent-mui.icons.question-mark]
    [reagent-mui.icons.refresh]
    [reagent-mui.icons.search]
    [reagent-mui.icons.send]
    [reagent-mui.icons.delete-icon]
    [reagent-mui.icons.upload]
    [reagent-mui.icons.warning]
    [reagent-mui.icons.warning-amber]
    [reagent-mui.icons.mouse-outlined]
    [reagent-mui.icons.arrow-forward-ios]
    [reagent-mui.icons.check-circle]
    [reagent-mui.icons.check]
    [reagent-mui.icons.close]
    [reagent-mui.icons.currency-pound]
    [reagent-mui.icons.content-copy]
    [reagent-mui.icons.north-east]
    [reagent-mui.components :as mui]
    [reagent-mui.lab.loading-button]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [applied-science.js-interop :as j]
    [kee-frame.router :as kf-router]
    [accountant.core :as accountant]
    ["@mui/material/styles" :as mui-styles]
    [reagent-mui.util]
    ["@mui/material/TextField" :as MuiTextField]
    [lambdaisland.uri :refer [query-string->map]]))

(def icon-directions-run reagent-mui.icons.directions-run/directions-run)
(def icon-playlist-play reagent-mui.icons.playlist-play/playlist-play)
(def icon-pending reagent-mui.icons.pending/pending)
(def icon-add reagent-mui.icons.add/add)
(def icon-admin-panel-settings-outlined reagent-mui.icons.admin-panel-settings-outlined/admin-panel-settings-outlined)
(def icon-arrow-back-sharp reagent-mui.icons.arrow-back-sharp/arrow-back-sharp)
(def icon-help-outline reagent-mui.icons.help-outline/help-outline)
(def icon-cloud-sync reagent-mui.icons.cloud-sync/cloud-sync)
(def icon-description-outlined reagent-mui.icons.description-outlined/description-outlined)
(def icon-download reagent-mui.icons.download/download)
(def icon-drafts-outlined reagent-mui.icons.drafts-outlined/drafts-outlined)
(def icon-error-outline reagent-mui.icons.error-outline/error-outline)
(def icon-edit reagent-mui.icons.edit/edit)
(def icon-expand-more reagent-mui.icons.expand-more/expand-more)
(def icon-history reagent-mui.icons.history/history)
(def icon-history-edu reagent-mui.icons.history-edu/history-edu)
(def icon-keyboard-arrow-up reagent-mui.icons.keyboard-arrow-up/keyboard-arrow-up)
(def icon-keyboard-arrow-down reagent-mui.icons.keyboard-arrow-down/keyboard-arrow-down)
(def icon-launch reagent-mui.icons.launch/launch)
(def icon-mail reagent-mui.icons.mail/mail)
(def icon-mail-outlined reagent-mui.icons.mail-outlined/mail-outlined)
(def icon-open-in-new reagent-mui.icons.open-in-new/open-in-new)
(def icon-question-mark reagent-mui.icons.question-mark/question-mark)
(def icon-link reagent-mui.icons.link/link)
(def icon-outbox reagent-mui.icons.outbox/outbox)
(def icon-priority-high reagent-mui.icons.priority-high/priority-high)
(def icon-add-circle reagent-mui.icons.add-circle/add-circle)
(def icon-account-balance reagent-mui.icons.account-balance/account-balance)
(def icon-person-outline reagent-mui.icons.person-outline/person-outline)
(def icon-refresh reagent-mui.icons.refresh/refresh)
(def icon-search reagent-mui.icons.search/search)
(def icon-send reagent-mui.icons.send/send)
(def icon-delete reagent-mui.icons.delete-icon/delete)
(def icon-arrow-forwards reagent-mui.icons.arrow-forward-ios/arrow-forward-ios)
(def icon-check reagent-mui.icons.check-circle/check-circle)
(def icon-check-base reagent-mui.icons.check/check)
(def icon-upload reagent-mui.icons.upload/upload)
(def icon-warning reagent-mui.icons.warning/warning)
(def icon-warning-amber reagent-mui.icons.warning-amber/warning-amber)
(def icon-mouse-outlined reagent-mui.icons.mouse-outlined/mouse-outlined)

(def icon-pound reagent-mui.icons.currency-pound/currency-pound)
(def icon-close reagent-mui.icons.close/close)

(def icon-copy reagent-mui.icons.content-copy/content-copy)

(def icon-arrow-out reagent-mui.icons.north-east/north-east)
(def loading-button reagent-mui.lab.loading-button/loading-button)

(defn ???_TO_BE_DEFINED_??? [message]
  [mui/alert {:severity :warning
              :icon (r/as-element [icon-help-outline {:fontSize :inherit}])}
   message])

(defn << [& args]
  @(rf/subscribe (vec args)))

(defn reg-fx+event
  "Registers an effect like re-frame's reg-fx, and registers
  an event with the same id that runs the effect."
  [id handler]
  (rf/reg-fx id handler)
  (rf/reg-event-fx id
    (fn [_ [_ v]]
      {:fx [[id v]]})))

(defn coerce-route [route]
  (cond
    (vector? route) route
    (keyword? route) [route]
    :else (assert "Route type not supported: " route)))

(rf/reg-fx ::navigate
  (fn [route]
    (kf-router/goto (coerce-route route))))

(rf/reg-event-fx ::navigate
  (fn [_ [_ route]]
    {::navigate route}))

(rf/reg-fx ::navigate-no-history
  (fn [route]
    (.replaceToken accountant/history (kf-router/url (coerce-route route)))))

(rf/reg-event-fx ::navigate-no-history
  (fn [_ [_ route]]
    {::navigate-no-history route}))

(rf/reg-sub ::path-params
  :<- [:kee-frame/route]
  (fn [route]
    (:path-params route)))

(rf/reg-sub ::query-params
  :<- [:kee-frame/route]
  (fn [route]
    (query-string->map (:query-string route))))

(defn- add-getAttribute!
  "Adds .getAttribute() method to a Material UI event.
  (Fork relies on that method, but some events don't provide it)."
  [evt]
  (when (nil? (j/get evt :getAttribute))
    (let [target (j/get evt :target)]
      (j/assoc! target :getAttribute
        (fn [attr]
          (j/get target (keyword attr)))))))

(defn mui-fork-args [fork-args]
  (let [{:keys [handle-change handle-blur]} fork-args]
    (assoc fork-args
      :handle-change (fn [evt]
                       (add-getAttribute! evt)
                       (handle-change evt))
      :handle-blur (fn [evt]
                     (add-getAttribute! evt)
                     (handle-blur evt)))))

(defn build-http [params]
  (merge
    {:timeout 8000
     :format (ajax/transit-request-format)
     :response-format (ajax/transit-response-format
                        {:keywords? true
                         ; Using a cljs UUID, instead of a Transit UUID, for no surprises. See:
                         ; https://groups.google.com/g/clojurescript/c/_B52tadgUgw/m/7r6uCh_EBgAJ
                         :handlers {"u" cljs.core/uuid}})}
    params))

(defn make-form-data [m]
  (let [form-data (js/FormData.)]
    (doseq [[k v] m]
      (.append form-data (name k) v))
    form-data))

(defn http-error-user-message [xhrio-failure-result]
  (let [{:keys [status]} xhrio-failure-result]
    ; see https://github.com/day8/re-frame-http-fx#step-3b-handling-on-failure
    (cond
      (= status 0) "Connection error" ; network failure
      (= status -1) "Connection error" ; timeout
      (< 500 status 599) "Unexpected server error"
      (= 404 status) "Unexpected error"
      (= 400 status) "Rejected request. Please check entered data"
      (= 413 status) "File is too big"
      :else "Unexpected error")))

(rf/reg-event-db ::show-message
  (fn [db [_ args]]
    (assoc db ::user-message args)))

(rf/reg-fx ::show-message
  (fn [args]
    (rf/dispatch [::show-message args])))

(rf/reg-fx ::notify-user-http-error
  (fn [{:keys [message result]}]
    (assert result (str ::notify-user-http-error " called without http result"))
    (rf/dispatch [::show-message {:severity :error
                                  :text (let [err (http-error-user-message result)]
                                          (if (some? message)
                                            (str message " (" err ")")
                                            err))}])))

(rf/reg-sub ::user-message
  (fn [db]
    (::user-message db)))

(defn user-notification-snackbar []
  (let [{:keys [severity text] :as notification} @(rf/subscribe [::user-message])]
    [mui/snackbar {:open (some? notification)
                   :autoHideDuration 5000
                   :on-close #(rf/dispatch [::show-message nil])}
     [mui/alert {:severity (or severity :info)}
      (or text "")]]))

; Fix reagent-mui.styles/create-theme
; See https://github.com/arttuka/reagent-material-ui/issues/41
(defn create-theme [options]
  (mui-styles/createTheme (reagent-mui.util/clj->js' options)))

(defn theme-spacing [arg]
  (fn [theme]
    (.spacing theme arg)))

(def original-mui-text-field
  (reagent-mui.util/adapt-react-class (.-default MuiTextField) "mui-text-field"))

(defn event-target-checked [onchange-event]
  (.. onchange-event -target -checked))

(defn event-target-value [onchange-event]
  (.. onchange-event -target -value))

(defn event-currentTarget [event]
  (.-currentTarget event))

(defn make-collapse-contents-full-width [props]
  (update props :sx
    assoc "& .MuiCollapse-wrapperInner" {:flex-grow 1}))