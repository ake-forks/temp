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
        case-id (parse-uuid (:case-id path-params))]
    (when (seq accounts)
      (xt-util/exec-tx xtdb-node
        (concat
          (case op
            :add (tx-fns/into* case-id [:bank-accounts :by-bank bank-id] accounts)
            :update (tx-fns/assoc* case-id [:bank-accounts :by-bank bank-id] accounts))
          (tx-fns/conj-unique case-id [:bank-accounts :bank-order] bank-id)
          (case-history/put-event {:event :updated.bank-accounts
                                   :case-id case-id
                                   :user user
                                   :op op
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
