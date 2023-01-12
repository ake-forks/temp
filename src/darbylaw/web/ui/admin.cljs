(ns darbylaw.web.ui.admin
  (:require [darbylaw.web.routes :as routes]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]
            [reagent-mui.x.data-grid :refer [data-grid]]
            [darbylaw.web.ui.mailing :as mailing]))

(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (assoc db :cases response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load!
  (fn [{:keys [db]} _]
    {:db (update db :config/case-view #(or % :card))
     :http-xhrio
     (ui/build-http
       {:method :get
        :uri "/api/cases"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-event-db ::set-case-view
  (fn [db [_ view]]
    (assoc db :config/case-view view)))

(rf/reg-sub ::cases
  (fn [db _]
    (:cases db)))

(rf/reg-sub ::case-view
  (fn [db _]
    (:config/case-view db)))


;; >> Cards

(defn case-item
  [{:keys [id reference fake loading?] :as _case
    {:keys [surname forename postcode]} :personal-representative}]
  [mui/card
   [mui/card-content
    [mui/stack {:direction :row
                :justify-content :space-between
                :align-items :center}
     [mui/card-action-area {:href (when-not loading?
                                    (kf/path-for [:dashboard {:case-id id}]))}
      [mui/stack
       [mui/typography {:sx {:fontSize 14} :color :text.secondary}
        (if-not loading?
          (str "case #" reference)
          [mui/skeleton {:width 100}])]
       [mui/typography {:variant :h5}
        (if-not loading?
          (str surname ", " forename)
          [mui/skeleton {:width 200}])]
       [mui/typography {:variant :h6 :color :text.secondary}
        (if-not loading?
          (str "at " postcode)
          [mui/skeleton {:width 130}])]]]
     [mui/stack
      [mui/tooltip {:title "See history"
                    :href (kf/path-for [:case-history {:case-id id}])}
       [mui/icon-button [ui/icon-history]]]]]]])

(defn no-cases-found
  []
  [mui/alert {:severity :info :sx {:z-index 999}}
   [mui/alert-title "No Cases Found"]
   "Maybe "
   [mui/link {:href (kf/path-for [:create-case])} "create"]
   " a new one?"])

(defn card-list [cases]
  [mui/container {:max-width :sm}
   [mui/stack {:spacing 2}
    (cond
      (empty? cases)
      [no-cases-found]

      (nil? cases)
      (for [id (range 3)]
        ^{:key id}
        [case-item {:loading? true}])

      :default
      (for [case cases]
        ^{:key (:id case)}
        [case-item case]))]])


;; >> DataGrid

(def columns
  [{:field :rowId :hide true}
   {:field :id :headerName "ID"}
   {:field :reference :headerName "Reference"}
   {:field :surname :headerName "Surname"}
   {:field :forename :headerName "Forename"}
   {:field :postcode :headerName "Post Code"}])

(defn to-rows
  [cases]
  (map (fn [{:keys [id reference] {:keys [surname forename postcode]} :personal-representative}]
         {:rowId id
          :id (str "#" id) ;; NOTE: The id must be a string for react
          :reference reference
          :surname surname
          :forename forename
          :postcode postcode})
       cases))

(defn data-grid-list
  [cases]
  (let [rows (to-rows cases)]
    [mui/box {:height 400}
     [data-grid {:loading (nil? cases)
                 :rows rows
                 :columns columns
                 :density :standard
                 :on-row-click #(rf/dispatch [::ui/navigate [:dashboard {:case-id (-> % .-row .-rowId str)}]])
                 :is-row-selectable (constantly false)
                 :components {:NoRowsOverlay 
                              #(r/as-element [mui/stack {:height "100%" :align-items :center :justify-content :center}
                                              [no-cases-found]])}
                 :sx {"& .MuiDataGrid-row" {:cursor :pointer}}}]]))


;; >> Panel

(defn admin-panel []
  (let [cases @(rf/subscribe [::cases])
        case-view @(rf/subscribe [::case-view])]
    [mui/container

     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :center
                 :sx {:mt "3rem"
                      :mb "1rem"}}
      [mui/typography {:variant :h4} "Cases"]
      [mui/stack {:direction :row
                  :spacing 2}
       [mui/button {:startIcon (r/as-element [ui/icon-add])
                    :href (str (kf/path-for [:create-case]) "?fake=true")
                    :variant :outlined}
        "Create fake case"]
       [mui/button {:startIcon (r/as-element [ui/icon-add])
                    :href (kf/path-for [:create-case])
                    :variant :outlined}
        "Create real case"]]]
     [mui/box {:border-bottom 1 :border-color :divider}
      [mui/tabs {:value (or case-view :card)
                 :on-change (fn [_ value] (rf/dispatch [::set-case-view (keyword value)]))}
       [mui/tab {:label "List" :value :card}]
       [mui/tab {:label "Table" :value :data-grid}]
       [mui/tab {:label "Mailing" :value :mail}]]]
     [mui/box {:margin-top 1}
      (case (or case-view :card)
        :card [card-list cases]
        :data-grid [data-grid-list cases]
        :mail [mailing/panel])]]))

(defn panel []
  (rf/dispatch [::load!])
  [admin-panel])

(defmethod routes/panels :admin-panel []
  [panel])
