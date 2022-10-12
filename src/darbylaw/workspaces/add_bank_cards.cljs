(ns darbylaw.workspaces.add-bank-cards
  (:require [reagent.core :as reagent]
            [reagent-mui.components :as mui]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]

            [clojure.spec.alpha :as s])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(def pair-regex #"\d{2}")
(def backspace-regex #"(\d{2}-)*")
(def group-regex #"(\d{2})-(\d{2})")
(def sort-regex #"(\d{2}-){2}(\d{2}-)")

(s/def ::numbers #(re-matches #"\d*" %))
(s/def ::first-pair #(re-matches pair-regex %))
(s/def ::group #(re-matches group-regex %))
(s/def ::pair-dash #(re-matches backspace-regex %))

(def sort-code (reagent/atom ""))
(def account-no (reagent/atom ""))
(def backspace (reagent/atom false))


(defn handle-acc-no [event]
  (let [val (-> event (.-target) (.-value))]
    (if (and (< (count val) 9) (s/valid? ::numbers val))
      (reset! account-no val))))


(defn handle-sort [event]
  (let [val (-> event (.-target) (.-value))]
    (if (false? @backspace)
      (if (< (count val) 9)
        (do (reset! sort-code val)
            (if
              (or (s/valid? ::first-pair @sort-code) (s/valid? ::group @sort-code))
              (reset! sort-code (str @sort-code "-"))))))

    (reset! backspace false)))

(defn handle-backspace [event]
  (if
    (= (.-key event) "Backspace")

    (if (s/valid? ::pair-dash @sort-code)
      (do (reset! sort-code (subs @sort-code 0 (- (count @sort-code) 2))) (reset! backspace true))
      (do (reset! sort-code (subs @sort-code 0 (- (count @sort-code) 1))) (reset! backspace true)))))

(def banks '("HSBC" "Santander" "Lloyds" "Monzo"))

(defn bank-form []
  [mui/container
   [mui/stack {:spacing 1}
    [mui/autocomplete {:options banks
                       :label "Bank Name"
                       :renderInput (react-component [props]
                                      [mui/text-field (merge {:label "Bank Name"} props)])}]
    [mui/text-field {:label "account number" :variant :filled
                     :inputProps {:maxLength 8 :value @account-no :on-change handle-acc-no}}]
    [mui/text-field {:label "sort code" :variant :filled
                     :inputProps {:value @sort-code :on-change handle-sort :on-key-down handle-backspace}}]]])




(ws/defcard bank-form
  (ct.react/react-card
    (reagent/as-element [bank-form])))
