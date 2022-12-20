(ns darbylaw.web.ui.buildingsociety.stage-add
  (:require
    [darbylaw.web.ui :as ui]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.buildingsociety.shared :as shared]))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  (let [current-case @(rf/subscribe [::case-model/current-case])]

    [mui/stack {:sx {:height 1}}
     [:form {:on-submit handle-submit :style {:height "100%"}}
      ;padding 5 = 40px (same as icon stack ^)
      [mui/stack {:justify-content :space-between
                  :sx {:height 1}}
       [mui/stack {:spacing 2}
        [mui/typography {:variant :h3
                         :sx {:mb 1}} "add a building society"]
        [form/buildsoc-select fork-args]
        [mui/typography {:variant :h5}
         (str "To the best of your knowledge, enter the details for your late "
           (-> current-case :deceased :relationship)
           (if-let [name (:buildsoc-id values)]
             (str "'s accounts with " name)
             "'s accounts."))]
        [mui/typography {:variant :body1}
         "If you don't have any account details, don't worry
         - building societies can usually retrieve the information they need
         with just a name and date of birth.
         If this is the case please check the box below."]
        [form/accounts-unknown fork-args]
        (if (:accounts-unknown values)
          [:<>]
          [form/account-array-component fork-args])]
       #_[mui/stack
          [form/submit-buttons]]]]]))

(defn panel []
  [form/form layout {:accounts [{}]}])


