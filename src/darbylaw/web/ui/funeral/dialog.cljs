(ns darbylaw.web.ui.funeral.dialog
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.account.dialog :as f-account]
    [darbylaw.web.ui.funeral.other.dialog :as f-other]
    [re-frame.core :as rf]))

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
