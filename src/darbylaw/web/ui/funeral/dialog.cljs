(ns darbylaw.web.ui.funeral.dialog
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.account.dialog :as f-account]
    [darbylaw.web.ui.funeral.other.dialog :as f-other]
    [re-frame.core :as rf]))

(defn close-button []
  []
  [mui/button {:on-click #(rf/dispatch [::funeral-model/hide-funeral-dialog])}
   "close"])

(defn view-other-expense
  [expense]
  [mui/stack {:spacing 1 :sx {:padding 2}}
   [mui/typography {:variant :h5}
    "other expense"]
   [mui/typography {:variant :body1}
    (:title expense)]
   [close-button]])

(defn view-expense [id]
  (let [expense @(rf/subscribe [::funeral-model/expense id])
        expense-type (:type expense)]))

(defn main-dialog []
  (let [dialog-info @(rf/subscribe [::funeral-model/dialog-info])]
    [mui/dialog {:open true
                 :maxWidth false
                 :fullWidth false}
     (case dialog-info
       :add-funeral-director [f-account/dialog {:type :add}]
       :add-other [f-other/dialog {:type :add}]
       :edit-account 
       (let [account @(rf/subscribe [::funeral-model/account])]
         [f-account/dialog {:type :edit :values account}])
       (let [expense
             @(rf/subscribe [::funeral-model/expense dialog-info])]
         [f-other/dialog {:type :edit :values expense}]))]))
