(ns darbylaw.api.pensions
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]))

(defn edit-pension [op {:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pension-id (parse-uuid (:pension-id path-params))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        (tx-fns/set-values pension-id (case op
                                        :private {:reference (:reference body-params)}
                                        :state {:reference (:reference body-params)
                                                :start-date (:start-date body-params)}))
        (case-history/put-event2 (merge {:case-id case-id
                                         :user user
                                         :subject :probate.case.pensions
                                         :op :updated
                                         :pension pension-id
                                         :pension-type op}
                                   (when (= op :private)
                                     {:institution (:provider body-params)})))))
    {:status 204}))

(defn add-pension [op {:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pension-id (random-uuid)
        provider (:provider body-params)]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge body-params
                     {:xt/id pension-id
                      :type :probate.pension
                      :probate.pension/case case-id
                      :pension-type op})]]
        (case-history/put-event2 (merge {:case-id case-id
                                         :user user
                                         :subject :probate.case.pensions
                                         :op :added
                                         :pension pension-id
                                         :pension-type op}
                                   (when (= op :private)
                                     {:institution provider})))))
    {:status 204}))

(def private-schema
  [:map
   [:provider :keyword]
   [:ni-number :string]
   [:reference {:optional true} :string]])

(def state-schema
  [:map
   [:tell-us-once {:optional true} :string]
   [:ni-number :string]
   [:reference {:optional true} :string]
   [:start-date {:optional true} :string]])

(defn routes []
  ["/case/:case-id/pension/"
   ["add-private" {:post {:handler (partial add-pension :private)
                          :parameters {:body private-schema}}}]
   ["add-state" {:post {:handler (partial add-pension :state)
                        :parameters {:body state-schema}}}]
   ["edit-private/:pension-id" {:post {:handler (partial edit-pension :private)
                                       :parameters {:body
                                                    [:map
                                                     [:provider :keyword]
                                                     [:reference {:optional true} :string]]}}}]
   ["edit-state/:pension-id" {:post {:handler (partial edit-pension :state)
                                     :parameters {:body
                                                  [:map
                                                   [:reference {:optional true} :string]
                                                   [:start-date {:optional true} :string]]}}}]])