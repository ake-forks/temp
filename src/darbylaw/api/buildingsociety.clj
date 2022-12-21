(ns darbylaw.api.buildingsociety
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]
    [reitit.coercion]
    [reitit.coercion.malli]
    [darbylaw.api.util.tx-fns :as tx-fns]))

(defn update-bank-accounts [op {:keys [xtdb-node user path-params body-params]}]
  (let [bank-id (:buildsoc-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))]
    (when (seq accounts)
      (xt-util/exec-tx xtdb-node
        (concat
          (case op
            :add (tx-fns/append case-id [:bank-accounts :by-bank bank-id] accounts)
            :update (tx-fns/set-value case-id [:bank-accounts :by-bank bank-id] accounts))
          (tx-fns/append-unique case-id [:bank-accounts :bank-order] [bank-id])
          (case-history/put-event {:event :updated.bank-accounts
                                   :case-id case-id
                                   :user user
                                   :op op
                                   :accounts accounts}))))
    {:status http/status-204-no-content}))



(defn update-buildsoc-accounts [op {:keys [xtdb-node user path-params body-params]}]
  (let [buildsoc-id (:buildsoc-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))]
    (when (seq accounts)
      (xt-util/exec-tx xtdb-node
        (concat
          (case op
            :add (tx-fns/append case-id [:building-societies :by-buildsoc buildsoc-id] accounts)
            :update (tx-fns/set-value case-id [:building-societies :by-buildsoc buildsoc-id] accounts))
          (tx-fns/append-unique case-id [:building-societies :buildsoc-order] [buildsoc-id])
          (case-history/put-event {:event :updated.buildsoc-accounts
                                   :case-id case-id
                                   :user user
                                   :op op
                                   :accounts accounts}))))
    {:status http/status-204-no-content}))

(def body-schema
  [:map
   [:buildsoc-id :keyword]
   [:accounts
    [:vector
     [:map
      [:accounts-unknown {:optional true} :boolean]
      [:roll-number {:optional true} :string]
      [:estimated-value {:optional true} :string]
      [:confirmed-value {:optional true} :string]]]]])

(defn routes []
  ["/buildingsociety"
   ["/:case-id"
    ["/add-buildsoc-accounts"
     {:post {:handler (partial update-buildsoc-accounts :add)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body body-schema}}}]

    ["/update-buildsoc-accounts"
     {:post {:handler (partial update-buildsoc-accounts :update)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body body-schema}}}]]])

