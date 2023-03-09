(ns darbylaw.api.buildingsociety
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.xtdb :as xt-util]
    [reitit.coercion]
    [reitit.coercion.malli]
    [darbylaw.api.util.tx-fns :as tx-fns]))

(defn update-buildsoc-accounts [op {:keys [xtdb-node user path-params body-params]}]
  (let [buildsoc-id (:buildsoc-id body-params)
        accounts-unknown (:accounts-unknown body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        asset-id {:type :probate.buildsoc-accounts
                  :case-id case-id
                  :buildsoc-id buildsoc-id}]
    (assert
      (or
        (not accounts-unknown)
        (empty? accounts)))
    (when (or accounts-unknown (seq accounts))
      (xt-util/exec-tx xtdb-node
        (concat
          (tx-fns/put-unique (merge asset-id
                               {:xt/id asset-id}))
          (case op
            :add
            (if accounts-unknown
              (tx-fns/set-values asset-id
                {:accounts nil
                 :accounts-unknown true})
              (concat
                (tx-fns/set-value asset-id [:accounts-unknown] false)
                (tx-fns/append asset-id [:accounts] accounts)))
            :update
            (tx-fns/set-values asset-id
              {:accounts accounts
               :accounts-unknown accounts-unknown}))
          (tx-fns/append-unique case-id [:buildsoc-accounts] [asset-id])
          (case-history/put-event2 {:case-id case-id
                                    :user user
                                    :subject :probate.case.buildsoc-accounts
                                    :op (case op
                                          :add :added
                                          :update :updated)
                                    :institution-type :buildsoc
                                    :institution buildsoc-id}))))
    {:status http/status-204-no-content}))

(def body-schema
  [:map
   [:buildsoc-id :keyword]
   [:accounts-unknown {:optional true} :boolean]
   [:accounts
    [:vector
     [:map
      [:roll-number {:optional true} :string]
      [:estimated-value {:optional true} :string]
      [:confirmed-value {:optional true} :string]]]]])

(def final-valuation-schema
  [:map
   [:buildsoc-id :keyword]
   [:accounts
    [:vector
     [:map
      [:roll-number :string]
      [:estimated-value {:optional true} :string]
      [:confirmed-value :string]]]]])

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
             :parameters {:body body-schema}}}]
    ["/value-buildsoc-accounts"
     {:post {:handler (partial update-buildsoc-accounts :update)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body final-valuation-schema}}}]]])
