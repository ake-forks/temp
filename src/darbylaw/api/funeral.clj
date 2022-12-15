(ns darbylaw.api.funeral
  (:require [clojure.tools.logging :as log]
            [reitit.coercion.malli]
            [xtdb.api :as xt]))

(def assoc-in__txn-fn
  '(fn [ctx eid ks v]
     (when-let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (assoc-in e ks v)]])))

(defn upsert-funeral-account [{:keys [xtdb-node path-params body-params]}]
  (log/info "Upsert funeral account")
  (let [case-id (parse-uuid (:case-id path-params))
        account-info body-params]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:xt/id ::assoc-in
                    :xt/fn assoc-in__txn-fn}]
         [::xt/fn ::assoc-in case-id
          [:funeral-account] account-info]]))
    {:status 200
     :body {:success? true}}))

(defn add-other-expense [{:keys [xtdb-node path-params body-params]}]
  (log/info "Add other funeral expense")
  (let [case-id (parse-uuid (:case-id path-params))
        expense-id (random-uuid)
        account-info body-params]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:xt/id ::assoc-in
                    :xt/fn assoc-in__txn-fn}]
         [::xt/fn ::assoc-in case-id
          [:funeral-expense expense-id] account-info]]))
    {:status 200
     :body {:id expense-id}}))

(defn update-other-expense [{:keys [xtdb-node path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        expense-id (parse-uuid (:expense-id path-params))
        expense-info body-params]
    (log/info (str "Update other funeral expense (" expense-id ")"))
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:xt/id ::assoc-in
                    :xt/fn assoc-in__txn-fn}]
         [::xt/fn ::assoc-in case-id
          [:funeral-expense expense-id] expense-info]]))
    {:status 200
     :body {:id expense-id}}))

(comment
  (let [xtdb-node darbylaw.xtdb-node/xtdb-node
        case-id-str "bb09a23c-d9bf-4e37-ac9f-b0bbf1e1939c"
        case-id (parse-uuid case-id-str)
        expense-id (parse-uuid "3a941e79-04f8-4207-9913-74d64321f160")]
    #_
    (update-other-expense
      {:xtdb-node xtdb-node
       :path-params {:case-id (str case-id)
                     :expense-id (str expense-id)}
       :body-params {:title "My Test Account"
                     :value 2345
                     :paid? true
                     :paid-by "Jim"}})
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:xt/id ::assoc-in
                    :xt/fn assoc-in__txn-fn}]
         [::xt/fn ::assoc-in case-id
          [:funeral-expense] nil]
         [::xt/fn ::assoc-in case-id
          [:funeral-account] nil]]))
    (xt/q (xt/db xtdb-node)
     {:find ['(pull case [*])]
      :where [['case :xt/id case-id]]})))

(def funeral-account-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid? {:optional true} :boolean]
   [:recipt {:optional true} :string]
   [:paid-by {:optional true} :string]
   [:invoice {:optional true} :string]])

(def other-expense-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid? {:optional true} :boolean]
   [:recipt {:optional true} :string]
   [:paid-by {:optional true} :string]])

(defn routes []
  ["/case/:case-id/funeral"
   ["/account"
    {:put {:handler upsert-funeral-account
           :coercion reitit.coercion.malli/coercion
           :parameters {:body funeral-account-schema}}}]
   ["/other"
    {:post {:handler add-other-expense
            :coercion reitit.coercion.malli/coercion
            :parameters {:body other-expense-schema}}}]
   ["/other/:expense-id"
    {:put {:handler update-other-expense
           :coercion reitit.coercion.malli/coercion
           :parameters {:body other-expense-schema}}}]])
