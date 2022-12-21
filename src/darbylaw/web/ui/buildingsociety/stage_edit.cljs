(ns darbylaw.web.ui.buildingsociety.stage-edit
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.buildingsociety.shared :as shared]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]))


(def new-data
  {:bath-building-society
   {:accounts
    [{:roll-number "1234A"
      :estimated-value "145.50"}
     {:roll-number "9876B"
      :estimated-value "290.50"}]}})

(def data {:accounts [{:roll-number "123AB", :estimated-value 150}
                      {:roll-number "123AB", :estimated-value 100}],
           :buildsoc-id "bath-building-society"})

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])
        dialog-data @(rf/subscribe [::model/get-dialog])
        buildsoc-id (:id dialog-data)]
    [:form {:on-submit handle-submit :style {:height "100%"}}
     [mui/dialog-title
      [shared/header buildsoc-id 0]]
     [mui/dialog-content
      [mui/box shared/narrow-dialog-props
       [mui/stack {:justify-content :space-between
                   :sx {:height 1}}
        [mui/stack {:spacing 1}

         [mui/typography {:variant :body1}
          (str "To the best of your knowledge, enter the details for your late "
            (-> current-case :deceased :relationship)
            (if-let [name (:buildsoc-id values)]
              (str "'s accounts with " name)
              "'s accounts."))]
         [form/accounts-unknown fork-args]
         (if (:accounts-unknown values)
           [:<>]
           [form/account-array-component fork-args])]]]]
     [mui/dialog-actions
      [shared/submit-buttons]]]))



(defn panel []
  [form/form layout data #(print %)])