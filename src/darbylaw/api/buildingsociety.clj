(ns darbylaw.api.buildingsociety
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]
    [reitit.coercion]
    [reitit.coercion.malli]
    [darbylaw.api.util.tx-fns :as tx-fns]))

(defn set-accounts-unknown [eid]
  (tx-fns/invoke ::set-accounts-unknown [eid]
    '(fn [eid]
       (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
         [[::xt/put (merge e {:accounts [] :accounts-unknown true})]]))))

(defn update-and-complete
  ([asset-id accounts] (tx-fns/invoke ::update-and-complete [asset-id accounts]
                         '(fn [ctx asset-id accounts]
                            (let [asset (xtdb.api/entity (xtdb.api/db ctx) asset-id)]
                              [[::xt/put (merge asset {:accounts accounts})]]))))
  ([asset-id accounts unknown] (tx-fns/invoke ::update-and-complete [asset-id accounts unknown]
                                 '(fn [ctx asset-id accounts unknown]
                                    (let [asset (xtdb.api/entity (xtdb.api/db ctx) asset-id)]
                                      [[::xt/put (merge asset {:accounts accounts
                                                               :accounts-unknown unknown})]])))))


(defn update-buildsoc-accounts [op {:keys [xtdb-node user path-params body-params]}]
  (let [buildsoc-id (:buildsoc-id body-params)
        accounts-unknown (:accounts-unknown body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        asset-id {:type :probate.buildsoc-accounts
                  :case-id case-id
                  :buildsoc-id buildsoc-id}]
    (when (seq accounts)
      (xt-util/exec-tx xtdb-node
        (concat
          (tx-fns/put-unique (merge asset-id
                               {:xt/id asset-id}))
          (case op
            :add (if (= true accounts-unknown)
                   (tx-fns/set-value asset-id [:accounts-unknown] accounts-unknown)
                   (tx-fns/append asset-id [:accounts] accounts))
            :update (if (= true accounts-unknown)
                      (set-accounts-unknown asset-id)
                      (tx-fns/set-value asset-id [:accounts] accounts))
            :complete (update-and-complete asset-id accounts accounts-unknown)
            :valuation (update-and-complete asset-id accounts))
          (tx-fns/append-unique case-id [:buildsoc-accounts] [asset-id])
          (case-history/put-event {:event :updated.buildsoc-accounts
                                   :case-id case-id
                                   :user user
                                   :op op
                                   :buildsoc-id buildsoc-id
                                   :accounts accounts}))))
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
    ["/complete-buildsoc-accounts"
     {:post {:handler (partial update-buildsoc-accounts :complete)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body body-schema}}}]
    ["/value-buildsoc-accounts"
     {:post {:handler (partial update-buildsoc-accounts :valuation)
             :coercion reitit.coercion.malli/coercion
             :parameters {:body final-valuation-schema}}}]]])
