(ns darbylaw.web.ui.dashboard
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.app-layout :as c]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.ui.bank-add :as bank]
    [darbylaw.api.bank-list :as bank-list]
    [re-frame.core :as rf]
    [reagent.core :as r]))


(rf/reg-sub ::route-params
  (fn [db _]
    (:path-params (:kee-frame/route db))))

(rf/reg-event-fx
  ::load-success
  (fn [{:keys [db]} [_ response]]
    {:db (assoc db :current-case response)}))

(rf/reg-event-db
  ::load-failure
  (fn [db [_ case-id result]]
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

(rf/reg-sub ::current-case
  (fn [db]
    (:current-case db)))

(rf/reg-sub ::bank-modal
  (fn [db _]
    (:modal/bank-modal db)))


(defn bank-item [bank case-id]
  (let [bank-data (bank-list/bank-by-id (:id bank))
        bank-name (:common-name bank-data)
        accounts (:accounts bank)]

    [mui/box
     [mui/card-action-area {:on-click #(rf/dispatch [::ui/navigate [:view-bank {:case-id case-id :bank-id (:id bank)}]])
                            :sx {:padding-top "0.5rem" :padding-bottom "0.5rem"}}
      [mui/stack {:spacing 0.5 :direction :row :justify-content :space-between}
       [mui/typography {:variant :h6} bank-name]
       [mui/typography {:variant :h6} (str "£" (reduce + (map (fn [account] (js/parseFloat (:estimated-value account))) accounts)))]]]
     [mui/divider {:variant "middle"}]]))

(defn add-bank []
  [mui/card-action-area {:on-click #(rf/dispatch [::bank/show-bank-modal]) :sx {:padding-top "0.5rem"}}
   [mui/stack {:direction :row :spacing 2 :align-items :baseline}
    [mui/typography {:variant :h5} "add bank account"]
    [ui/icon-add]]])

(defn bank-card [current-case case-id]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:font-weight 600}} "bank accounts"]
    [mui/divider]
    (for [bank (:bank-accounts current-case)]
      ^{:key (:id bank)}
      [bank-item bank case-id])
    [add-bank]]])

(defn panel []
  (let [case-id (-> @(rf/subscribe [::route-params])
                  :case-id)
        current-case @(rf/subscribe [::current-case])
        bank-modal-open @(rf/subscribe [::bank-modal])]
    (assert case-id)
    (rf/dispatch [::load! case-id])
    [mui/container {:style {:max-width "100%"}}
     [c/navbar]
     [mui/container {:maxWidth :xl :class (styles/main-content)}
      [mui/stack {:spacing 3}
       [mui/stack {:direction :row :justify-content :space-between :align-items :baseline}
        [mui/typography {:variant :h2}
         (if (nil? (:deceased current-case))
           (str "welcome")
           (str "your "
             (-> current-case :deceased :relationship (clojure.string/lower-case))
             "'s estate"))]
        [mui/typography {:variant :h3} (if (nil? current-case) [mui/skeleton {:width "5rem"}] (str "case # " (subs (-> current-case :id .toString) 0 6)))]]
       [mui/box {:sx {:width 1100 :height 150 :background-color "#808080" :borderRadius "4px"}}]]
      [mui/stack {:spacing 3 :sx {:padding-top "2rem"}}
       [mui/typography {:variant :h3} "estate details"]
       [mui/dialog
        {:open (boolean bank-modal-open)
         :maxWidth :md
         :fullWidth true}
        [bank/modal]]
       [mui/stack {:direction :row :spacing 2}
        [mui/grid {:container true :spacing 2 :columns 3}
         [mui/grid {:item true :xs 1}
          (r/as-element [bank-card current-case case-id])]]

        [mui/stack {:spacing 2}
         [mui/box {:sx {:width 200 :height 250 :background-color "#808080" :borderRadius "4px"}}]
         [mui/box {:sx {:width 200 :height 100 :background-color "#808080" :borderRadius "4px"}}]]]]]
     [c/footer]]))


(defmethod routes/panels :dashboard-panel [] [panel])

