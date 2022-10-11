(ns darbylaw.web.ui
  (:require
    [reagent-mui.icons.account-balance]
    [reagent-mui.icons.add]
    [reagent-mui.icons.help-outline]
    [reagent-mui.icons.add-circle]
    [reagent-mui.icons.person-outline]
    [reagent-mui.components :as mui]
    [reagent-mui.lab.loading-button]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [darbylaw.web.routes :as routes]
    [ajax.core :as ajax]))

(def icon-add reagent-mui.icons.add/add)
(def icon-help-outline reagent-mui.icons.help-outline/help-outline)
(def icon-add-circle reagent-mui.icons.add-circle/add-circle)
(def icon-account-balance reagent-mui.icons.account-balance/account-balance)
(def icon-person-outline reagent-mui.icons.person-outline/person-outline)

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

(defn form-handle-change-fn [{:keys [set-handle-change]}]
  "The handle-change function provided by the Fork library is not compatible
  with Material UI. We need to provide our own."
  (fn [evt _]
    (set-handle-change {:value (.. evt -target -value)
                        :path [(keyword (.. evt -target -name))]})))

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