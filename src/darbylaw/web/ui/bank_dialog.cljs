(ns darbylaw.web.ui.bank-dialog
  (:require [re-frame.core :as rf]
            [reagent-mui.components :as mui]
            [darbylaw.api.bank-list :as list]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.bank-model :as bank-model]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.progress-bar :as prog-bar]
            [darbylaw.web.ui.bank-add :as add]
            [darbylaw.web.ui.bank-confirmation-view :as confirm]
            [reagent.core :as r]))



;stages
; 0, no accounts added yet (show edit modal)
; 1, not marked complete (show edit modal)
; 2, marked complete (show notification confirmation modal)
; 3, notification sent
; 4, valuations received and need to be added (bank-confirmation modal)
; 5, bank complete (can view all)

(defn get-stage []
  (let [banks (-> @(rf/subscribe [::case-model/current-case]) :bank-accounts)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        current-bank (filter #(= (:id %) bank-id) banks)
        banks-complete @(rf/subscribe [::bank-model/banks-complete])]
    (if (some #(= bank-id %) banks-complete)
      (if (contains? (get-in (:accounts (first current-bank)) [0]) :confirmed-value) 5 4)
      1)))





(defn bank-progress-bar []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        banks (-> @(rf/subscribe [::case-model/current-case]) :bank-accounts)
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
  [mui/stack {:direction :column}
   [mui/stack {:direction :row :spacing 1}
    [mui/stack {:spacing 2 :width "50%"}
     [mui/typography {:variant :h6}
      "The notification process is complete.
      You can view copies of all correspondence using the buttons below."]
     [mui/box
      [mui/typography "todo -> add buttons to display pdfs"]]]
    [mui/stack {:direction :row :spacing 1 :style {:align-self :baseline}}
     [mui/box
      [mui/typography "accounts"]
      (map
        (fn [acc]
          [mui/stack {:direction :row :spacing 1}
           [mui/typography {:variant :h6} (str "sort code: " (:sort-code acc))]
           [mui/typography {:variant :h6} (str "account number: " (:account-number acc))]
           [mui/typography {:variant :h6} (str "value: £" (:confirmed-value acc))]])
        (:accounts (first current-bank)))]]]
   [mui/button {:on-click #(rf/dispatch [::bank-model/hide-bank-dialog])
                :variant :contained
                :style {:margin-top "25%"}} "close"]])

(defn pdf-dialog []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
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

(defn standard-dialog []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
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
         1 [add/dialog-with-values
            {:accounts (:accounts (first current-bank))
             :bank-id (name bank-id)}]
         5 [bank-complete (list/bank-label bank-id) current-bank]))]))


(defn base-dialog []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        banks (:bank-accounts current-case)
        bank-id @(rf/subscribe [::bank-model/bank-dialog])
        case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)
        current-bank (filter #(= (:id %) bank-id) banks)]
    (rf/dispatch [::case-model/load-case! case-id])
    [mui/box
     (if (= :add-bank bank-id)
       [add/dialog]
       (if (not (nil? bank-id))
         (case (get-stage)
           1 [standard-dialog]
           4 [pdf-dialog]
           5 [standard-dialog])))]))




