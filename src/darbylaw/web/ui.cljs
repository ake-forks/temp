(ns darbylaw.web.ui
  (:require
    [reagent-mui.icons.account-balance]
    [reagent-mui.icons.add]
    [reagent-mui.icons.help-outline]
    [reagent-mui.icons.error-outline]
    [reagent-mui.icons.add-circle]
    [reagent-mui.components :as mui]
    [reagent-mui.lab.loading-button]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [darbylaw.web.routes :as routes]
    [ajax.core :as ajax]
    [applied-science.js-interop :as j]))

(def icon-add reagent-mui.icons.add/add)
(def icon-help-outline reagent-mui.icons.help-outline/help-outline)
(def icon-error-outline reagent-mui.icons.error-outline/error-outline)
(def icon-add-circle reagent-mui.icons.add-circle/add-circle)
(def icon-account-balance reagent-mui.icons.account-balance/account-balance)

(def loading-button reagent-mui.lab.loading-button/loading-button)

(defn ???_TO_BE_DEFINED_??? [message]
  [mui/alert {:severity :warning
              :icon (r/as-element [icon-help-outline {:fontSize :inherit}])}
   message])

(defn do-navigate [nav-fn route]
  (cond
    (keyword? route) (nav-fn route nil)
    (sequential? route) (let [[route-name params] route]
                          (nav-fn route-name params))
    :else (assert false
            (str "Handler must be a keyword or a vector of 2, but is " route))))

(rf/reg-fx ::navigate
  (fn [route]
    (do-navigate routes/navigate! route)))

(rf/reg-event-fx ::navigate
  (fn [_ [_ route]]
    {::navigate route}))

(rf/reg-fx ::navigate-no-history
  (fn [route]
    (do-navigate routes/navigate-replacing! route)))

(rf/reg-event-fx ::navigate-no-history
  (fn [_ [_ route]]
    {::navigate-no-history route}))

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
                         ; bidi needs a cljs UUID, instead of a Transit UUID. See:
                         ; https://groups.google.com/g/clojurescript/c/_B52tadgUgw/m/7r6uCh_EBgAJ
                         :handlers {"u" cljs.core/uuid}})}
    params))