(ns darbylaw.web.ui.pensions.dashboard
  (:require
    [darbylaw.web.ui.app-layout :as l]
    [darbylaw.web.ui.pensions.model :as model]
    [darbylaw.web.ui.pensions.dialog :as dialog]
    [darbylaw.web.ui.notification.model :as notification-model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui :refer (<<)]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]))

(defn pensions-card []
  (let [pensions (group-by :pension-type (<< ::model/pensions))
        private (:private pensions)
        state (:state pensions)
        case-id (<< ::case-model/case-id)]
    [:<>
     [dialog/dialog]
     [l/asset-card {:title "pensions"}
      (when (some? private)
        [:<>
         [mui/typography {:sx {:font-weight 600 :pt 1}} "private"]
         (for [{:keys [id provider]} private]
           ^{:key id}
           [l/asset-item {:title provider
                          :on-click #(rf/dispatch [::notification-model/open
                                                   {:notification-type :pension
                                                    :case-id case-id
                                                    :provider provider
                                                    :pension-type :private}])
                          :indent 1}])])
      (when (some? state)
        [:<>
         [mui/typography {:sx {:font-weight 600 :pt 1}} "state"]
         (for [{:keys [id]} state]
           ^{:key id}
           [l/asset-item {:title "DWP" :indent 1}])])
      [l/menu-asset-add-button
       model/anchor
       {"add private pension" #(rf/dispatch [::model/show-dialog nil :private :add])
        "add state pension" #(print "state")}]]]))