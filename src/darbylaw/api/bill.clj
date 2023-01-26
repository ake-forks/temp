(ns darbylaw.api.bill
  (:require [darbylaw.api.bill.data :as bill-data]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.util.xtdb :as xt-util]
            [xtdb.api :as xt]))

(defn add-bill [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bill-id (random-uuid)
        bill-data (select-keys body-params bill-data/bill-props)]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge bill-data
                     {:xt/id bill-id
                      :type :probate.bill
                      :probate.bill/case case-id})]]
        (tx-fns/append case-id [:bills] [bill-id])
        (case-history/put-event {:event :updated.bills
                                 :case-id case-id
                                 :user user
                                 :op :add
                                 :company (:company bill-data)})))))

(defn routes []
  ["/case/:case-id/bill" {:post {:handler add-bill
                                 :parameters {:body bill-data/bill-schema}}}])

(comment
  (require 'darbylaw.xtdb-node)
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull doc [*])]
      :where [[doc :type :probate.bill]]}))