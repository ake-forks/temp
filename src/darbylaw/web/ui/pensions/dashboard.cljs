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
         (doall
           (for [{:keys [id provider]} private]
             ^{:key id}
             [l/asset-item {:title (model/get-label provider)
                            :on-click #(rf/dispatch [::notification-model/open
                                                     {:notification-type :pension
                                                      :case-id case-id
                                                      :provider provider
                                                      :pension-type :private
                                                      :asset-id (str id)}])
                            :indent 1}]))])
      (when (some? state)
        [:<>
         [mui/typography {:sx {:font-weight 600 :pt 1}} "state"]
         (for [{:keys [id]} state]
           ^{:key id}
           [l/asset-item {:title "DWP"
                          :on-click #(rf/dispatch [::notification-model/open
                                                   {:notification-type :pension
                                                    :case-id case-id
                                                    :provider :state
                                                    :pension-type :state
                                                    :asset-id (str id)}])
                          :indent 1}])])
      [l/menu-asset-add-button
       model/anchor
       (merge
         {"add private pension" #(rf/dispatch [::model/show-dialog nil :private :add])}
         (when (empty? (filter #(= :state (:pension-type %)) (<< ::model/pensions)))
           {"add state pension" #(rf/dispatch [::model/show-dialog nil :state :add])}))]]]))