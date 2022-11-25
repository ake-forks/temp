(ns darbylaw.web.ui.dashboard
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.app-layout :as c]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.ui.bank-add :as bank]
    [darbylaw.web.ui.bank-modal :as bank-modal]
    [darbylaw.api.bank-list :as bank-list]
    [darbylaw.web.ui.progress-bar :as progress-bar]
    [darbylaw.web.ui.overview-tile :as overview]
    [darbylaw.web.ui.tasks-tile :as tasks]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]))


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

(defn bank-item [bank]
  (let [bank-data (bank-list/bank-by-id (:id bank))
        bank-id (:id bank)
        accounts (:accounts bank)
        modal @(rf/subscribe [::bank-modal])]
    [mui/box
     [mui/card-action-area {:on-click #(rf/dispatch [::bank/show-bank-modal bank-id])
                            :sx {:padding-top "0.5rem" :padding-bottom "0.5rem"}}
      [mui/stack {:direction :row :spacing 1}
       [mui/box {:component :img
                 :src (str "/images/bank-logos/" (:icon bank-data))
                 :sx {:width 25 :mr 1}}]
       [mui/stack {:spacing 0.5 :direction :row :justify-content :space-between :style {:width "100%"}}
        [mui/typography {:variant :body1} (:common-name bank-data)]]
       [mui/typography {:variant :body1 :sx {:font-weight :bold}}
        (str "£" (format/format "%.2f"
                   (reduce + (map (fn [account]
                                    (if (clojure.string/blank? (:estimated-value account))
                                      0
                                      (js/parseFloat (:estimated-value account)))) accounts))))]]]

     [mui/dialog
      {:open (if (= modal bank-id) true false)
       :maxWidth false
       :fullWidth false}
      [bank-modal/base-modal]]]))

(defn add-bank []
  [mui/card-action-area {:on-click #(rf/dispatch [::bank/show-bank-modal :add-bank]) :sx {:padding-top "0.5rem"}}
   [mui/stack {:direction :row :spacing 1}
    [mui/typography {:variant :body1
                     :sx {:color :primary.main
                          :font-weight 600}}
     "add bank account"]
    [ui/icon-add {:color :primary}]]])

(defn bank-card [current-case]
  (let [accounts (:bank-accounts current-case)]
    [mui/card
     [mui/card-content
      [mui/typography {:variant :h6 :font-weight 600}
       "bank accounts"]
      [mui/divider]
      (interpose
        ^{:key 1} ;; Ugly hack but works? What would a good solution look like?
        [mui/divider]
        (for [bank accounts]
          ^{:key (:id bank)}
          [bank-item bank]))
      [mui/divider]
      [add-bank]]]))

(defn heading [current-case]
  [mui/box {:sx {:background-color theme/off-white :padding-bottom {:xs "2rem" :xl "4rem"}}}
   [mui/container {:maxWidth :xl :class (styles/main-content)}
    [mui/stack {:spacing 3}
     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :baseline
                 :sx {:padding-top {:xs "0.5rem" :xl "2rem"}}}
      [mui/typography {:variant :h4}
       (if (nil? (:deceased current-case))
         (str "welcome")
         (str "your "
           (-> current-case :deceased :relationship (clojure.string/lower-case))
           "'s estate"))]
      [mui/typography {:variant :h6}
       (if (nil? current-case)
         [mui/skeleton {:width "5rem"}]
         (str "case " (:reference current-case :reference)))]]
     [progress-bar/progress-bar]]]])

(defn content [current-case]
  (let [bank-modal @(rf/subscribe [::bank-modal])]
    [mui/container {:maxWidth :xl}
     [mui/stack {:spacing 2 :sx {:pt "1rem" :pb "2rem"}}
      [mui/typography {:variant :h5} "estate details"]
      [mui/dialog
       {:open (= bank-modal :add-bank)
        :maxWidth :md
        :fullWidth true}
       [bank-modal/base-modal]]

      [mui/stack {:direction :row :spacing 1 :style {:margin-top "0.5rem"}}
       [mui/grid {:container true :spacing 1 :columns 3
                  :style {:width "70%"}}
        [mui/grid {:item true :xs 1}
         (r/as-element [bank-card current-case])]]

       [mui/stack {:spacing 2 :style {:width "30%"}}
        [tasks/tasks-tile]
        [overview/overview-card]]]]]))

(defn panel []
  (let [case-id (-> @(rf/subscribe [::route-params])
                  :case-id)
        current-case @(rf/subscribe [::current-case])]
    (assert case-id)
    (rf/dispatch [::load! case-id])
    (rf/dispatch [::bank/mark-bank-complete :load])
    [mui/box
     [c/navbar]
     [heading current-case]
     [content current-case]
     [c/footer]]))

(defmethod routes/panels :dashboard-panel [] [panel])

