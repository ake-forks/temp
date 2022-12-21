(ns darbylaw.api.bank
  (:require [reitit.coercion]
            [reitit.coercion.malli]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]))

(defn update-bank-accounts [op {:keys [xtdb-node user path-params body-params]}]
  (let [bank-id (:bank-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        asset-id {:type :probate.case.asset.bank-accounts
                  :case-id case-id
                  :bank-id bank-id}]
    (when (seq accounts)
      (xt-util/exec-tx xtdb-node
        (concat
          (tx-fns/put-unique (merge asset-id
                               {:xt/id asset-id}))
          (case op
            :add (tx-fns/append asset-id [:accounts] accounts)
            :update (tx-fns/set-value asset-id [:accounts] accounts))
          (tx-fns/append-unique case-id [:bank-accounts] [asset-id])
          (case-history/put-event {:event :updated.bank-accounts
                                   :case-id case-id
                                   :user user
                                   :op op
                                   :bank-id bank-id
                                   :accounts accounts}))))
    {:status http/status-204-no-content}))

(def body-schema
  [:map
   [:bank-id :keyword]
   [:accounts
    [:vector
     [:map
      [:sort-code :string]
      [:account-number :string]
      [:estimated-value {:optional true} :string]
      [:joint-check {:optional true} :boolean]
      [:joint-info {:optional true} :string]
      [:confirmed-value {:optional true} :string]]]]])

(defn routes []
  ["/bank"
   ["/:case-id"
    ["/add-bank-accounts"
     {:post {:handler (partial update-bank-accounts :add)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body body-schema}}}]

    ["/update-bank-accounts"
     {:post {:handler (partial update-bank-accounts :update)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body body-schema}}}]]])
