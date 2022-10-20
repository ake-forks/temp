(ns darbylaw.workspaces.add-bank-cards
  (:require [reagent.core :as r]
            [reagent-mui.components :as mui]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [darbylaw.workspaces.workspace-styles :as styles]
            [clojure.spec.alpha :as s]
            [darbylaw.workspaces.workspace-icons :as icon])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(def pair-regex #"\d{2}")
(def backspace-regex #"(\d{2}-)*")
(def group-regex #"(\d{2})-(\d{2})")
(def sort-regex #"(\d{2}-){2}(\d{2}-)")

(s/def ::numbers #(re-matches #"\d*" %))
(s/def ::first-pair #(re-matches pair-regex %))
(s/def ::group #(re-matches group-regex %))
(s/def ::pair-dash #(re-matches backspace-regex %))


(def sort-code (r/atom ""))
(def account-no (r/atom ""))
(def acc-value (r/atom ""))
(def backspace (r/atom false))
(def bank (r/atom nil))
(def saved (r/atom {:account-no nil :sort-code nil :value nil}))

(defn handle-bank [_ value]
  (reset! bank value))


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

(defn handle-add []
  (reset! saved {:account-no @account-no
                 :sort-code @sort-code
                 :value @acc-value})
  (reset! sort-code "")
  (reset! account-no "")
  (reset! acc-value ""))

(def banks ["HSBC" "Santander" "Lloyds" "Monzo"])

(defn select-bank []
  [mui/autocomplete {:options banks
                     :label "Bank Name"
                     :on-change handle-bank
                     :value @bank
                     :renderInput (react-component [props]
                                    [mui/text-field (merge {:label "Bank Name"} props)])}])

(defn add-account []
  [mui/stack {:spacing 0.75 :style {:margin-top "0.5rem" :margin-bottom "1rem"}}
   [mui/text-field {:label "account number" :variant :filled :required true
                    :inputProps {:maxLength 8 :value @account-no :on-change handle-acc-no}}]
   [mui/text-field {:label "sort code" :variant :filled :required true
                    :inputProps {:value @sort-code :on-change handle-sort :on-key-down handle-backspace}}]

   [mui/text-field {:label "estimated value" :variant :filled
                    :inputProps {:value @acc-value
                                 :on-change #(reset! acc-value (-> % .-target .-value))}
                    :InputProps {:start-adornment (r/as-element [mui/input-adornment {:position :start} "£"])}}]])


(defn account []
  [mui/stack {:spacing 1 :style {:margin-top "1rem" :margin-bottom "1rem"}}
   [mui/divider {:variant "middle"}]
   [mui/stack {:direction :row :justify-content :right}
    [mui/icon-button
     [icon/mui-edit]]
    [mui/icon-button
     [icon/mui-delete {:onClick #(reset! saved {:account-no nil})}]]]
   [mui/text-field {:disabled true :label "account number" :variant :filled
                    :inputProps {:value (:account-no @saved)}}]
   [mui/text-field {:disabled true :label "sort code" :variant :filled
                    :inputProps {:value (:sort-code @saved)}}]
   [mui/text-field {:disabled true :label "estimated value" :variant :filled
                    :inputProps {:value (:value @saved)}
                    :InputProps {:start-adornment (r/as-element [mui/input-adornment {:position :start} "£"])}}]
   [mui/divider {:variant "middle" :style {:margin-top "1rem"}}]])



(defn bank-form []
  [mui/container {:class (styles/mui-bank)}
   [mui/stack {:spacing 1}
    [mui/typography {:variant :h3} "add bank"]
    [select-bank]
    [mui/typography {:variant :h6}
     "To the best of your knowledge, enter the details for all of your father's accounts"
     (if (nil? @bank) "." (str " with " @bank "."))]
    (if (nil? (:account-no @saved))
      [:<>]
      [account])
    [add-account]
    [mui/button {:variant :text :full-width false :onClick handle-add
                 :start-icon (r/as-element [icon/mui-add])}
     (str "add another " @bank " account")]
    [mui/typography "Once you have added all the accounts for " @bank ", save to go back to the dashboard.
     Unsure if you've provided all the details? Don't worry - banks will handle all accounts held under your relative's name once contacted,
      even if you haven't provided the details explicitly."]
    [mui/stack {:style {:margin-top "1rem"} :direction :row :spacing 1 :justify-content :space-between}
     [mui/button {:variant :contained :full-width true} "come back later"]
     [mui/button {:variant :contained :full-width true} "save and finish"]]]])






(defn single-account []
  [mui/container {:class (styles/mui-bank)}
   [mui/stack {:spacing 1}
    [mui/typography {:variant :h3} "add bank account"]
    [select-bank]
    [mui/typography {:variant :h6} "Enter your father's account details to the best of your knowledge."]
    [add-account]
    [mui/button {:variant :contained :full-width true} "save"]]])






(ws/defcard add-bank
  (ct.react/react-card
    (r/as-element [bank-form])))

(ws/defcard add-account
  (ct.react/react-card
    (r/as-element [single-account])))


