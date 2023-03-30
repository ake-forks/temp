(ns darbylaw.api.pensions
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]))

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
        (case-history/put-event2 {:case-id case-id
                                  :user user
                                  :subject :probate.case.pensions
                                  :op :added
                                  :pension pension-id
                                  :pension-type op
                                  :institution provider})))
    {:status 204}))


(def private-schema
  [:map
   [:provider :keyword]
   [:ni-number :string]
   [:reference {:optional true} :string]])

(defn routes []
  ["/case/:case-id/pension/"
   ["add-private" {:post {:handler (partial add-pension :private)
                          :parameters {:body private-schema}}}]])