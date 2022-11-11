(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]))

(def date--schema
  [:re #"^\d{4}-\d{2}-\d{2}$"])

(def personal-representative--schema
  [:map
   [:title :string]
   [:forename :string]
   [:middlename {:optional true} :string]
   [:surname :string]
   [:date-of-birth date--schema]

   [:email :string]
   [:phone :string]

   [:flat {:optional true} :string]
   [:building {:optional true} :string]
   [:street-number {:optional true} :string]
   [:street1 :string]
   [:street2 {:optional true} :string]
   [:town :string]
   [:postcode :string]])

(def personal-representative--props
  (mapv first (drop 1 personal-representative--schema)))

(def deceased--schema
  [:map
   [:relationship :string]

   [:registration-district :string]
   [:administrative-area :string]
   [:entry-number :string]

   [:date-of-death date--schema]
   [:place-of-death :string]

   [:forename :string]
   [:middlename {:optional true} :string]
   [:surname :string]
   [:sex [:enum "male" "female"]]
   [:maiden-name {:optional true} :string]
   [:date-of-birth date--schema]
   [:place-of-birth :string]
   [:occupation :string]

   [:name-of-informant :string]
   [:cause-of-death :string]
   [:name-of-doctor-certifying :string]
   [:name-of-registrar :string]])

(def put-with-tx-data__txn-fn
  '(fn [ctx m]
     (let [tx (xtdb.api/indexing-tx ctx)]
       [[::xt/put (assoc m
                    :timestamp (::xt/tx-time tx)
                    :tx-id (::xt/tx-id tx))]])))

(comment
  (def res (xt/submit-tx darbylaw.xtdb-node/xtdb-node
             [[::xt/put {:xt/id ::put-with-tx-data
                         :xt/fn put-with-tx-data__txn-fn}]
              [::xt/fn ::put-with-tx-data {:xt/id :my-event
                                           :type :event}]]))
  res
  (xt/await-tx darbylaw.xtdb-node/xtdb-node res)
  (xt/tx-committed? darbylaw.xtdb-node/xtdb-node res)
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) :my-event))

(defn put-event [txns event case-id]
  (into txns
    [[::xt/put {:xt/id ::put-with-tx-data
                :xt/fn put-with-tx-data__txn-fn}]
     [::xt/fn ::put-with-tx-data {:xt/id (random-uuid)
                                  :type :event
                                  :subject-type :probate.case
                                  :event event
                                  :ref/probate.case.id case-id}]]))

(defn create-case [{:keys [xtdb-node body-params]}]
  (let [case-id (random-uuid)
        pr-info-id (random-uuid)
        pr-info (get body-params :personal-representative)]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (-> [[::xt/put {:type :probate.case
                        :xt/id case-id
                        :ref/personal-representative.info.id pr-info-id}]
             [::xt/put (merge
                         pr-info
                         {:type :probate.personal-representative.info
                          :xt/id pr-info-id})]]
          (put-event :created case-id))))
    {:status 200
     :body {:id case-id}}))

(def merge__txn-fn
  '(fn [ctx eid m]
     (when-let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (merge e m)]])))

(defn update-deceased-info [{:keys [xtdb-node path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        deceased-info body-params]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (-> [[::xt/put {:xt/id ::merge
                        :xt/fn merge__txn-fn}]
             [::xt/fn ::merge case-id {:deceased.info deceased-info}]]
          (put-event :updated.deceased.info case-id))))
    {:status 200
     :body deceased-info}))

(def update-ref__txn-fn
  '(fn [ctx eid ref-k m]
     (let [db (xtdb.api/db ctx)
           entity (xtdb.api/entity db eid)]
       (assert entity (str "entity not found: " eid))
       (let [refed-eid (get entity ref-k)
             _ (assert refed-eid (str "no ref in entity: " ref-k))
             refed-entity (xtdb.api/entity db refed-eid)]
         (assert refed-entity (str "refed entity not found: " refed-eid))
         [[::xt/put (merge m (select-keys refed-entity [:xt/id :type]))]]))))

(defn update-pr-info [{:keys [xtdb-node path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pr-info body-params]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (-> [[::xt/put {:xt/id ::update-ref
                        :xt/fn update-ref__txn-fn}]
             [::xt/fn ::update-ref case-id :ref/personal-representative.info.id pr-info]]
          (put-event :updated.personal-representative.info case-id))))
    {:status 200
     :body pr-info}))

(comment
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/put {:xt/id ::update-ref
                :xt/fn update-ref__txn-fn}]])

  (update-pr-info {:xtdb-node darbylaw.xtdb-node/xtdb-node
                   :path-params {:case-id "be757deb-9cda-4424-a1a2-00e7176dc579"}
                   :body-params {:forename "changed!"}})
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) ::update-ref)
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/evict ::update-ref]])

  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"be757deb-9cda-4424-a1a2-00e7176dc579"),)

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

(defn add-bank-accounts [{:keys [xtdb-node path-params body-params]}]
  (let [bank-id (:bank-id body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        e (xt/entity (xt/db xtdb-node) case-id)]
    ;check if id exists in bank-accounts
    (if (empty? (filter #(= bank-id (:id %)) (:bank-accounts e)))
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::add-bank-txn
                      :xt/fn add-bank-txn}]
           [::xt/fn ::add-bank-txn case-id accounts bank-id]]))
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::update-bank-txn
                      :xt/fn update-bank-txn}]
           [::xt/fn ::update-bank-txn case-id accounts bank-id]])))
    {:status 204}))

(defn get-cases [{:keys [xtdb-node]}]
  (ring/response
    (->> (xt/q (xt/db xtdb-node)
           {:find [(list 'pull 'case
                     [:xt/id
                      {:ref/personal-representative.info.id
                       personal-representative--props}])]
            :where '[[case :type :probate.case]]})
      (map (fn [[case]]
             (-> case
               (clojure.set/rename-keys {:xt/id :id})
               (clojure.set/rename-keys {:ref/personal-representative.info.id :personal-representative})))))))

(comment
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"51127427-6ff1-4093-9929-c2c9990c796e"))

(def get-case__query
  {:find [(list 'pull 'case [:xt/id
                             {:ref/personal-representative.info.id
                              personal-representative--props}
                             :deceased.info
                             :bank-accounts])]
   :where '[[case :type :probate.case]
            [case :xt/id case-id]]
   :in '[case-id]})

(defn get-case [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        results (xt/q (xt/db xtdb-node) get-case__query case-id)]
    (assert (= 1 (count results)))
    (ring/response
      (clojure.set/rename-keys (ffirst results)
        {:xt/id :id
         :ref/personal-representative.info.id :personal-representative
         :deceased.info :deceased}))))

(defn get-case-history [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        results (xt/q (xt/db xtdb-node)
                  '{:find [(pull event [*]) timestamp]
                    :where [[event :type :event]
                            [event :ref/probate.case.id case-id]
                            [event :timestamp timestamp]]
                    :order-by [[timestamp :asc]]
                    :in [case-id]}
                  case-id)]
    (ring/response
      (->> results
        (map #(-> %
                first
                (select-keys [:xt/id
                              :timestamp
                              :event])
                (clojure.set/rename-keys
                  {:xt/id :id})))))))

(defn get-event [{:keys [xtdb-node path-params]}]
  (let [event-id (parse-uuid (:event-id path-params))
        event (xt/q (xt/db xtdb-node)
                '{:find [(pull event [:tx-id
                                      :ref/probate.case.id])]
                  :where [[event :type :event]
                          [event :xt/id event-id]]
                  :in [event-id]}
                event-id)
        {:keys [tx-id]
         case-id :ref/probate.case.id} (ffirst event)
        db-before (xt/db xtdb-node {::xt/tx {::xt/tx-id (dec tx-id)}})
        case-before (xt/q db-before get-case__query case-id)
        db-after (xt/db xtdb-node {::xt/tx {::xt/tx-id tx-id}})
        case-after (xt/q db-after get-case__query case-id)]
    (ring/response
      {:case-before (ffirst case-before)
       :case-after (ffirst case-after)})))

(comment
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull event [*])]
      :where [[event :type :event]]})
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node {::xt/tx {::xt/tx-id -1}})
    '{:find [(pull event [*])]
      :where [[event :type :event]]})
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/delete #uuid"b32ecf9a-7f9e-45cd-89f7-3a9abecdfddd"]]))

(defn routes []
  [["/case" {:post {:handler create-case
                    :coercion reitit.coercion.malli/coercion
                    :parameters {:body [:map
                                        [:personal-representative
                                         personal-representative--schema]]}}}]

   ["/case/:case-id" {:get {:handler get-case}}]

   ["/case/:case-id/history" {:get {:handler get-case-history}}]
   ["/event/:event-id" {:get {:handler get-event}}]

   ["/case/:case-id/personal-representative"
    {:put {:handler update-pr-info
           :coercion reitit.coercion.malli/coercion
           :parameters {:body personal-representative--schema}}}]

   ["/case/:case-id/deceased"
    {:put {:handler update-deceased-info
           :coercion reitit.coercion.malli/coercion
           :parameters {:body deceased--schema}}}]

   ["/case/:case-id/add-bank-accounts" {:post {:handler add-bank-accounts
                                               :coercion reitit.coercion.malli/coercion
                                               :parameters {:body
                                                            [:map
                                                             [:bank-id :keyword]
                                                             [:accounts
                                                              [:vector
                                                               [:map
                                                                [:sort-code :string]
                                                                [:account-number :string]
                                                                [:estimated-value :string]
                                                                [:joint-check {:optional true} :boolean]
                                                                [:joint-info {:optional true} :string]]]]]}}}]

   ["/cases" {:get {:handler get-cases}}]])
