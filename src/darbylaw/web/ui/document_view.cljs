(ns darbylaw.web.ui.document-view
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]))



(rf/reg-sub ::pdf-view
  (fn [db]
    (:modal/pdf-view db)))

;data format {:name name :source path-to-source}
(rf/reg-event-db ::show-pdf
  (fn [db [_ data]]
    (assoc-in db [:modal/pdf-view] data)))

(rf/reg-event-db ::hide-pdf
  (fn [db]
    (assoc-in db [:modal/pdf-view] nil)))



(defn view-pdf []
  (let [data @(rf/subscribe [::pdf-view])]
    [mui/stack {:spacing 0.5 :style {:height "80vh"}}
     [mui/button {:variant :outlined
                  :on-click #(rf/dispatch [::hide-pdf])}
      "close"]
     [:iframe {:src (:source data)
               :width "100%"
               :height "100%"}]]))

(defn toggle-pdf [data pdf-view]
  (let [active? (= (:name pdf-view) (:name data))]
    (if active?
      (rf/dispatch [::hide-pdf])
      (rf/dispatch [::show-pdf data]))))

(defn pdf-button [{:keys [name source]} pdf-view]
  "data format:
     {:name name
      :source path-to-source}"
  (let [active? (= (:name pdf-view) name)]
    [mui/button {:variant (if active? :outlined :contained)
                 :full-width true
                 :onClick #(toggle-pdf {:name name
                                        :source source}
                             pdf-view)}
     (if active? "close" name)]))

(defn view-pdf-dialog [{:keys [buttons]}]
  "param format:
     {:buttons
       [{:name name
         :source path-to-source}
        {:name name
         :source path-to-source}]}"
  (let [pdf-view @(rf/subscribe [::pdf-view])]
    [mui/stack {:spacing 1
                :style {:background-color :white}}
     [mui/box {:style {:width "100%"}}
      (if (not (nil? pdf-view))
        [view-pdf]
        [mui/stack {:spacing 1}
         (map-indexed
           (fn [idx btn-params]
             ^{:key idx}
             [mui/box
              [pdf-button btn-params pdf-view]])
           buttons)])]]))


