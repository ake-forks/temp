(ns darbylaw.web.ui.bank-modal
  (:require [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [darbylaw.api.bank-list :as list]
            [darbylaw.web.util.subscriptions :as sub]
            [darbylaw.web.ui.progress-bar :as prog-bar]
            [darbylaw.web.ui.bank-add :as add]
            [darbylaw.web.ui.bank-confirmation-view :as confirm]
            [reagent.core :as r]))

(rf/reg-sub ::banks-complete
  (fn [db _]
    (:banks-complete db)))


;stages
; 0, no accounts added yet (show edit modal)
; 1, not marked complete (show edit modal)
; 2, marked complete (show notification confirmation modal)
; 3, notification sent
; 4, valuations received and need to be added (bank-confirmation modal)
; 5, bank complete (can view all)

(defn get-stage []
  (let [banks (-> @(rf/subscribe [::sub/current-case]) :bank-accounts)
        bank-id @(rf/subscribe [::sub/bank-modal])
        current-bank (filter #(= (:id %) bank-id) banks)
        banks-complete @(rf/subscribe [::banks-complete])]
    (if (some #(= bank-id %) banks-complete)
      (if (contains? (get-in (:accounts (first current-bank)) [0]) :confirmed-value) 5 4)
      1)))





(defn bank-progress-bar []
  (let [current-case @(rf/subscribe [::sub/current-case])
        banks (-> @(rf/subscribe [::sub/current-case]) :bank-accounts)
        current-bank (filter #(= (:id %) :barclays-bank-plc) banks)]

    [mui/stepper {:alternative-label true}
     [mui/step
      [mui/step-label {:icon (if (= (get-stage) 1)
                               (prog-bar/get-icon 2)
                               (prog-bar/get-icon 4))} "add accounts"]]
     [mui/step
      [mui/step-label {:icon (if (= (get-stage) 1)
                               (prog-bar/get-icon 0)
                               (prog-bar/get-icon 4))} "send notification"]]
     [mui/step
      [mui/step-label {:icon (case (get-stage)
                               1 (prog-bar/get-icon 0)
                               4 (prog-bar/get-icon 2)
                               5 (prog-bar/get-icon 4))} "input valuations"]]]))


(defn bank-complete [bank-name current-bank]
  (let [pdf (r/atom false)]
    [mui/stack {:direction :column}
     [mui/stack {:direction :row :spacing 1}
      [mui/stack {:spacing 2 :width "50%"}
       [mui/typography {:variant :h6}
        "The notification process is complete.
        You can view copies of all correspondence using the buttons below."]
       [mui/stack {:direction :row :spacing 1 :style {:align-self :baseline}}
        [mui/button {:variant :contained :on-click #(print @pdf)} "notification letter"]
        [mui/button {:variant :contained :on-click #(reset! pdf true)} "bank's response"]]]

      (if (true? (boolean @pdf))
        [mui/box {:style {:width "50%"}}
         [:iframe {:src "/Example-bank-confirmation-letter.pdf" :width "100%" :height "100%"}]]
        [mui/box
         [mui/typography "accounts"]
         (map
           (fn [acc]
             [mui/stack {:direction :row :spacing 1}
              [mui/typography {:variant :h6} (str "sort code: " (:sort-code acc))]
              [mui/typography {:variant :h6} (str "account number: " (:account-number acc))]
              [mui/typography {:variant :h6} (str "value: £" (:confirmed-value acc))]])
           (:accounts (first current-bank)))])]
     [mui/button {:on-click #(rf/dispatch [::sub/hide-bank-modal])
                  :variant :contained
                  :style {:margin-top "25%"}} "close"]]))

(defn pdf-modal []
  (let [current-case @(rf/subscribe [::sub/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::sub/bank-modal])
        case-id (-> @(rf/subscribe [::sub/route-params]) :case-id)
        current-bank (filter #(= (:id %) bank-id) banks)]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "800px"
                        :width "1300px"}}
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h4} (list/bank-label bank-id)]]
     [mui/stack {:direction :row :spacing 1 :style {:height "inherit"}}
      [mui/stack {:spacing 1 :style {:width "50%"}}
       [mui/card {:style {:padding "1rem" :margin-bottom "1rem"}}
        [bank-progress-bar]]
       [confirm/bank-confirmation-panel]]
      [mui/box {:style {:width "50%"}}
       [:iframe {:src "/Example-bank-confirmation-letter.pdf" :width "100%" :height "100%"}]]]]))

(defn standard-modal []
  (let [current-case @(rf/subscribe [::sub/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::sub/bank-modal])
        case-id (-> @(rf/subscribe [::sub/route-params]) :case-id)
        current-bank (filter #(= (:id %) bank-id) banks)]
    [mui/stack {:spacing 1
                :style {:padding "2rem"
                        :background-color :white
                        :height "700px"
                        :width "1130px"}}
     ;header
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h4} (list/bank-label bank-id)]
      [mui/card {:style {:padding "1rem" :margin-bottom "1rem"}}
       [bank-progress-bar]]]
     (if (not (nil? bank-id))
       (case (get-stage)
         1 [add/modal-with-values
            {:accounts (:accounts (first current-bank))
             :bank-id (name bank-id)}]
         5 [bank-complete (list/bank-label bank-id) current-bank]))]))


(defn base-modal []
  (let [current-case @(rf/subscribe [::sub/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::sub/bank-modal])
        case-id (-> @(rf/subscribe [::sub/route-params]) :case-id)
        current-bank (filter #(= (:id %) bank-id) banks)]
    (rf/dispatch [::sub/load! case-id])
    [mui/box
     (if (= :add-bank bank-id)
       [add/modal]
       (if (not (nil? bank-id))
         (case (get-stage)
           1 [standard-modal]
           4 [pdf-modal]
           5 [standard-modal])))]))




