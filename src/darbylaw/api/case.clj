(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]
            [darbylaw.web.util.bank :as bank-util]))

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

(def concat-in__txn-fn
  '(fn [ctx eid ks value]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (update-in e ks concat value)]])))

(def merge-in__txn-fn
  '(fn [ctx eid ks value]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (update-in e ks merge value)]])))


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

(defn add-bank [{:keys [xtdb-node path-params body-params]}]
  (let [bank-name (:bank-name body-params)
        accounts (:accounts body-params)
        case-id (parse-uuid (:case-id path-params))
        bank-data (bank-util/get-bank-by-common-name bank-name)
        e (xt/entity (xt/db xtdb-node) case-id)]
    ;check if id exists in bank-accounts
    (if (empty? (filter #(= (:id bank-data) (:id %)) (:bank-accounts e)))
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::add-bank-txn
                      :xt/fn add-bank-txn}]
           [::xt/fn ::add-bank-txn case-id accounts (:id bank-data)]]))
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::update-bank-txn
                      :xt/fn update-bank-txn}]
           [::xt/fn ::update-bank-txn case-id accounts (:id bank-data)]])))
    {:status 200
     :body {:bank-data body-params}}))


(comment
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) "test123")
  (add-bank-test darbylaw.xtdb-node/xtdb-node
    "test123"
    "Santander"
    [{:sort-code "5" :account-number "5" :estimated-value "7"}])

  (let [e (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) "test123")
        id :santander]
    (empty? (filter #(= id (:id %)) (:bank-accounts e))))


  (let [e (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) "test123")
        bank-id :santander
        accounts [{:sort-code "3" :account-number "3" :estimated-value "300"}]]
    (assoc e :bank-accounts (mapv #(if (= (:id %) bank-id)
                                     (update % :accounts concat accounts) %) (:bank-accounts e))))


  ;CREATE
  (let [xtdb-node darbylaw.xtdb-node/xtdb-node]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:xt/id "test123"
                    :bank-accounts []}]])))





  (defn add-first-bank [xtdb-node id bank-name values]
    (let [bank-data (bank-util/get-bank-by-common-name bank-name)]
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id id
                      :bank-accounts (vector {:id (:id bank-data) :accounts values})}]]))))

  (add-first-bank darbylaw.xtdb-node/xtdb-node "test123"
    "Santander" [{:sort-code "222" :account-number "222" :estimated-value "200"}])




  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"57984cd9-c386-4682-9c59-80694eea67ff")
  (def entry
    {:id 1234
     :bank-accounts [{:id :aberdeen-standard-investments, :accounts
                      [{:sort-code "5", :account-number "5", :estimated-value "5"}]}
                     {:id :charter-savings-bank, :accounts
                      [{:sort-code "1", :account-number "1", :estimated-value "1"}]}]})
  (def accs (:bank-accounts entry))
  (print (update-in entry [:bank-accounts] (some
                                             #(if (= :aberdeen-standard-investments (:id %)))
                                             accs)
           {:id :aberdeen-standard-investments :accounts {:new "values"}}))
  (def new-entry (mapv
                   #(if (= :aberdeen-standard-investments (:id %))
                      (update % :accounts conj {:new "vals"})
                      %)
                   (:bank-accounts entry)))
  (identity new-entry)

  ())




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
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"51127427-6ff1-4093-9929-c2c9990c796e")
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"162f1c25-ac28-45a9-9663-28e2accf11dc"))

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

   ["/case/:case-id/add-bank-accounts" {:post {:handler add-bank
                                               :coercion reitit.coercion.malli/coercion
                                               :parameters {:body
                                                            [:map
                                                             [:bank-name :string]
                                                             [:accounts
                                                              [:vector
                                                               [:map
                                                                [:sort-code :string]
                                                                [:account-number :string]
                                                                [:estimated-value :string]
                                                                [:joint-check {:optional true} :boolean]
                                                                [:joint-info {:optional true} :string]]]]]}}}]

   ["/cases" {:get {:handler get-cases}}]])
