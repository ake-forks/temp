(ns darbylaw.api.bank
  (:require [xtdb.api :as xt]
            [darbylaw.api.case :as case-api]
            [reitit.coercion]
            [reitit.coercion.malli]))

(def update-bank-txn
  '(fn [ctx eid accounts bank-id]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)
           new-data (mapv #(if (= (:id %) bank-id)
                             (update % :accounts (comp vec concat) accounts)
                             %)
                      (:bank-accounts e))]
       [[::xt/put (assoc e :bank-accounts new-data)]])))

(def add-bank-txn
  '(fn [ctx eid accounts bank-id]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (update-in e [:bank-accounts] conj {:id bank-id :accounts accounts})]])))

(defn add-bank-accounts [{:keys [xtdb-node user path-params body-params]}]
  (let [bank-id (:bank-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        e (xt/entity (xt/db xtdb-node) case-id)]
    (if (empty? accounts)
      ;if no accounts are sent then abort transaction
      nil
      ;otherwise, check if id exists in bank-accounts
      (if (empty? (filter #(= bank-id (:id %)) (:bank-accounts e)))
        ;if bank-id does not exist, add new entry under that id
        (xt/await-tx xtdb-node
          (xt/submit-tx xtdb-node
            (-> [[::xt/put {:xt/id ::add-bank-txn
                            :xt/fn add-bank-txn}]
                 [::xt/fn ::add-bank-txn case-id accounts bank-id]]
              (case-api/put-event :updated.bank-accounts case-id user))))
        ;if it does, update existing entry for that id
        (xt/await-tx xtdb-node
          (xt/submit-tx xtdb-node
            (-> [[::xt/put {:xt/id ::update-bank-txn
                            :xt/fn update-bank-txn}]
                 [::xt/fn ::update-bank-txn case-id accounts bank-id]]
              (case-api/put-event :updated.bank-accounts case-id user))))))
    {:status 200
     :body body-params}))

(def edit-bank-txn
  '(fn [ctx eid accounts bank-id]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)
           new-data (mapv #(if (= (:id %) bank-id)
                             (assoc % :accounts accounts)
                             %)
                      (:bank-accounts e))
           filtered-data (filterv #(not-empty (:accounts %)) new-data)]
       [[::xt/put (assoc-in e [:bank-accounts] filtered-data)]])))

(defn update-bank-accounts [{:keys [xtdb-node user path-params body-params]}]
  (let [bank-id (:bank-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        e (xt/entity (xt/db xtdb-node) case-id)]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (-> [[::xt/put {:xt/id ::edit-bank-txn
                        :xt/fn edit-bank-txn}]
             [::xt/fn ::edit-bank-txn case-id accounts bank-id]]
            (case-api/put-event :updated.bank-accounts case-id user))))
    {:status 200
     :body {:accounts accounts :case-id (.toString case-id)}}))

(defn routes []
  ["/bank"
   ["/:case-id"
    ["/add-bank-accounts"
     {:post {:handler add-bank-accounts
             :coercion reitit.coercion.malli/coercion
             :parameters {:body
                          [:map
                           [:bank-id :keyword]
                           [:accounts
                            [:vector
                             [:map
                              [:sort-code :string]
                              [:account-number :string]
                              [:estimated-value {:optional true} :string]
                              [:joint-check {:optional true} :boolean]
                              [:joint-info {:optional true} :string]]]]]}}}]
    ["/update-bank-accounts"
     {:post {:handler update-bank-accounts
             :coercion reitit.coercion.malli/coercion
             :parameters {:body
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
                              [:confirmed-value {:optional true} :string]]]]]}}}]]])
