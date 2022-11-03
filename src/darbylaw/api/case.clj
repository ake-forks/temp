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
   [:dob date--schema]

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

(defn create-case [{:keys [xtdb-node body-params]}]
  (let [case-id (random-uuid)
        pr-info-id (random-uuid)
        pr-info (get body-params :personal-representative)]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:type :probate.case
                    :xt/id case-id
                    :ref/personal-representative.info.id pr-info-id}]
         [::xt/put (merge
                     pr-info
                     {:type :probate.personal-representative.info
                      :xt/id pr-info-id})]]))
    {:status 200
     :body {:id case-id}}))

(def merge__txn-fn
  '(fn [ctx eid m]
     (when-let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (merge e m)]])))

(defn update-case [{:keys [xtdb-node path-params body-params]}]
  (let [deceased-info (:deceased body-params)]
    (when deceased-info
      (let [case-id (parse-uuid (:case-id path-params))
            deceased-info-id (random-uuid)]
        (xt/await-tx xtdb-node
          (xt/submit-tx xtdb-node
            [[::xt/put {:xt/id ::merge
                        :xt/fn merge__txn-fn}]
             [::xt/fn ::merge case-id {:ref/deceased.info.id deceased-info-id}]
             [::xt/put (merge
                         deceased-info
                         {:type :probate.deceased.info
                          :xt/id deceased-info-id})]])))
      {:status 204})))

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
        [[::xt/put {:xt/id ::update-ref
                    :xt/fn update-ref__txn-fn}]
         [::xt/fn ::update-ref case-id :ref/personal-representative.info.id pr-info]]))
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

  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"be757deb-9cda-4424-a1a2-00e7176dc579")
  ,)

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

(defn get-case [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        results (xt/q (xt/db xtdb-node)
                  {:find [(list 'pull 'case [:xt/id
                                             {:ref/personal-representative.info.id
                                              personal-representative--props}
                                             {:ref/deceased.info.id
                                              [:forename
                                               :surname
                                               :relationship]}])]
                   :where '[[case :type :probate.case]
                            [case :xt/id case-id]]
                   :in '[case-id]}
                  case-id)]
    (assert (= 1 (count results)))
    (ring/response
      (clojure.set/rename-keys (ffirst results)
        {:xt/id :id
         :ref/personal-representative.info.id :personal-representative
         :ref/deceased.info.id :deceased}))))

(defn routes []
  [["/case" {:post {:handler create-case
                    :coercion reitit.coercion.malli/coercion
                    :parameters {:body [:map
                                        [:personal-representative
                                         personal-representative--schema]]}}}]

   ["/case/:case-id" {:patch {:handler update-case}
                      :get {:handler get-case}}]
                     ;:coercion reitit.coercion.malli/coercion}}]
                     ;:parameters {:path {:case-id uuid?}
                     ;             :body [:map
                     ;                    [:deceased any?]]}}}]

   ["/case/:case-id/personal-representative"
    {:put {:handler update-pr-info
           :coercion reitit.coercion.malli/coercion
           :parameters {:body personal-representative--schema}}}]

   ["/cases" {:get {:handler get-cases}}]])
