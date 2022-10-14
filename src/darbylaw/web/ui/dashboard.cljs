(ns darbylaw.web.ui.dashboard
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.components :as c]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [darbylaw.web.routes :as routes]
    [re-frame.core :as rf]
    [clojure.pprint :as pp]
    [darbylaw.web.ui.bank :as bank]))



(rf/reg-sub ::route-params
  (fn [db _]
    (:route-params db)))

(defn get-case-id []
  (do
    (pp/pprint (str "sub: " @(rf/subscribe [::route-params])))
    (pp/pprint (str "case-id: " (:case-id @(rf/subscribe [::route-params]))))))



(rf/reg-event-fx ::load-success
  (fn [{:keys [db]} [_ response]]
    (println "success" response)
    {:db (assoc db :current-case response)}))

(rf/reg-event-db
  ::load-failure
  (fn [db [_ case-id result]]
    ;; result is a map containing details of the failure
    (assoc db :failure-http-result result :case-id case-id)))


(rf/reg-event-fx ::load!
  (fn [_ [_ case-id]]
    {:dispatch [::get-case! case-id]}))

(rf/reg-event-fx ::get-case!
  (fn [_ [_ case-id]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id)
        :on-success [::load-success]
        :on-failure [::load-failure case-id]})}))

(enable-console-print!)

(def banks {"Santander" 1500 "HSBC" 895})
(def utilities {"British Gas" -300 "EON" 0 "three" 20})

(defn asset-item [name amount]
  [mui/box
   [mui/card-action-area {:onClick #(println name) :sx {:padding-top "0.5rem" :padding-bottom "0.5rem"}}
    [mui/stack {:spacing 0.5 :direction :row :justify-content :space-between}
     [mui/typography {:variant :h6} name]
     [mui/typography {:variant :h6} "£" amount]]]
   [mui/divider {:variant "middle"}]])

(defn add-asset [type]
  [mui/card-action-area {:onClick #(bank/add-bank-toggle) :sx {:padding-top "0.5rem"}}
   [mui/stack {:direction :row :spacing 2 :align-items :baseline}
    [mui/typography {:variant :h5} "add " type]
    [ui/icon-add]]])



(defn asset-card [type data]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:font-weight 600}} type " accounts"]
    [mui/divider]
    (map (fn [[key value]] (asset-item key value)) data)

    [add-asset type]]])





(defn card-holder [] [mui/box {:sx {:width "100%" :height 200 :background-color "#d3d3d3" :border-radius "4px"}}])

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(defn panel []
  (let [case-id (-> @(rf/subscribe [::route-params])
                  :case-id)
        current-case @(rf/subscribe [::current-case])]
    (assert case-id)
    (rf/dispatch [::load! case-id])

    [mui/container {:style {:max-width "100%"}}
     [c/navbar]



     [mui/container {:maxWidth :xl :class (styles/main-content)}

      [mui/stack {:spacing 3}
       [mui/stack {:direction :row :justify-content :space-between :align-items :baseline}

        [mui/typography {:variant :h1} (str "your " (-> current-case :deceased :relationship)) "'s estate"]
        [mui/typography {:variant :h2} (str "case no. " (-> current-case :id))]]
       [mui/box {:sx {:width 1100 :height 150 :background-color "#808080" :border-radius "4px"}}]]

      [mui/stack {:spacing 3 :sx {:padding-top "2rem"}}
       [mui/typography {:variant :h3} "estate details"]
       [mui/stack {:direction :row :spacing 2}
        [mui/grid {:container true :spacing 2 :columns 3}
         [mui/grid {:item true :xs 1}
          [asset-card "bank" banks]]
         [mui/grid {:item true :xs 1}
          [asset-card "utility" utilities]]
         [mui/grid {:item true :xs 1}
          [asset-card "bank" banks]]]





        [mui/stack {:spacing 2}
         [mui/box {:sx {:width 200 :height 250 :background-color "#808080" :border-radius "4px"}}]
         [mui/box {:sx {:width 200 :height 100 :background-color "#808080" :border-radius "4px"}}]]]]]


     [c/footer]]))



(defmethod routes/panels :dashboard-panel [] [panel])

