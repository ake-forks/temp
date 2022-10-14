(ns darbylaw.web.ui.admin
  (:require [darbylaw.web.routes :as routes]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [ajax.core :as ajax]
            [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]
            [reagent-mui.x.data-grid :refer [data-grid]]
            [reagent-mui.util :refer [wrap-clj-function]]))

(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (println "success" response)
    (assoc db :cases response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load!
  (fn [{:keys [db]} _]
    {:db (update db :config/case-view #(or % :card))
     :http-xhrio
     {:method :get
      :uri "/api/cases"
      :timeout 8000
      :response-format (ajax/transit-response-format)
      :on-success [::load-success]
      :on-failure [::load-failure]}}))

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

;; TODO: Clean up?
(defn case-item
  [{:keys [id] {:keys [surname forename postcode]} :personal-representative}]
  [mui/card
   [mui/card-action-area {:onClick #(rf/dispatch [::ui/navigate [:dashboard {:case-id (.toString id)}]])}
    [mui/card-content
     [mui/stack {:direction :row :justify-content :space-between :align-items :center}
      [mui/stack
       [mui/typography {:sx {:fontSize 14} :color :text.secondary}
         (str "#" id)]
       [mui/typography {:variant :h5}
         surname ", " forename]
       [mui/typography {:variant :h6 :color :text.secondary}
        "at " postcode]]
      [mui/skeleton {:variant :circular :align :right :width "5em" :height "5em"}]]]]])

(defn card-list
  [cases]
  (cond
    ;; TODO: Improve skeleton & "no cases"
    (nil? cases) [mui/card [mui/skeleton]]
    (empty? cases) [mui/typography "No cases found"]

    :default
    [mui/container {:max-width :sm}
     [mui/stack {:spacing 2}
      (for [case cases]
        ^{:key (:id case)}
        [case-item case])]]))


;; >> DataGrid

(def columns
  [{:field :rowId :hide true}
   {:field :id :headerName "ID"}
   {:field :surname :headerName "Surname"}
   {:field :forename :headerName "Forename"}
   {:field :postcode :headerName "Post Code"}])

(defn to-rows
  [cases]
  (map (fn [{:keys [id] {:keys [surname forename postcode]} :personal-representative}]
         {:rowId id
          :id (str "#" id) ;; NOTE: The id must be a string for react
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
                 :is-row-selectable (constantly false)}]]))


;; >> Panel

(defn admin-panel []
  (let [cases @(rf/subscribe [::cases])
        case-view @(rf/subscribe [::case-view])]
    [mui/container

     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :center}
      [mui/typography {:variant :h1} "Cases"]
      [mui/button {:startIcon (r/as-element [ui/icon-add])
                   :onClick #(rf/dispatch [::ui/navigate :create-case])}
       "Create case"]]
     [mui/box {:border-bottom 1 :border-color :divider}
      [mui/tabs {:value case-view
                 :on-change (fn [_ value] (rf/dispatch [::set-case-view (keyword value)]))}
       [mui/tab {:label "List" :value :card}]
       [mui/tab {:label "Grid" :value :data-grid}]]]
     [mui/box {:margin-top 1}
      (if (= case-view :data-grid)
        [data-grid-list cases]
        [card-list cases])]]))

(defn panel []
  (rf/dispatch [::load!])
  [admin-panel])

(defmethod routes/panels :admin-panel []
  [panel])
