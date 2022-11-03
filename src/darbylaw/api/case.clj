(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]
            [darbylaw.web.util.bank :as bank-util]))

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




(comment
  (let [xtdb-node darbylaw.xtdb-node/xtdb-node]
    (get-bank-id "Britannia" xtdb-node)))


(def update-in__txn-fn
  '(fn [ctx eid ks value]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (update-in e ks concat value)]])))

(def merge-in__txn-fn
  '(fn [ctx eid ks value]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)]
       [[::xt/put (update-in e ks merge value)]])))


(defn add-bank [{:keys [xtdb-node path-params body-params]}]
  (let [bank-info (get body-params :bank-info)
        bank-name (:bank-name bank-info)
        accounts (:account bank-info)
        case-id (parse-uuid (:case-id path-params))
        bank-data (bank-util/get-bank-by-common-name bank-name)]
    (when bank-info
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::update-in
                      :xt/fn update-in__txn-fn}]
           [::xt/put {:xt/id ::merge-in
                      :xt/fn merge-in__txn-fn}]
           [::xt/fn ::update-in case-id [:assets (:id bank-data) :accounts] accounts]
           [::xt/fn ::merge-in case-id [:assets (:id bank-data)] {:type "asset.bank" :name bank-name}]]))
      {:status 200
       :body {:id case-id :data bank-info}})))


(defn get-cases [{:keys [xtdb-node]}]
  (ring/response
    (->> (xt/q (xt/db xtdb-node)
           '{:find [(pull case [:xt/id
                                {:ref/personal-representative.info.id
                                 [:forename
                                  :surname
                                  :postcode]}])]
             :where [[case :type :probate.case]]})
      (map (fn [[case {pr-info :ref/personal-representative.info.id}]]
             (-> case
               (clojure.set/rename-keys {:xt/id :id})
               (clojure.set/rename-keys {:ref/personal-representative.info.id :personal-representative})))))))




(comment
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"51127427-6ff1-4093-9929-c2c9990c796e")
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"162f1c25-ac28-45a9-9663-28e2accf11dc")
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"23362950-f80d-48f0-8851-75299b9176ec"))

(defn get-case [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        results (xt/q (xt/db xtdb-node)
                  '{:find [(pull case [*
                                       {:ref/personal-representative.info.id
                                        [*]}
                                       {:ref/deceased.info.id
                                        [*]}])]

                    :where [[case :xt/id case-id]]
                    :in [case-id]}
                  case-id)]
    (assert (= 1 (count results)))
    (ring/response
      (-> (clojure.set/rename-keys (ffirst results)
            {:xt/id :id
             :ref/personal-representative.info.id :personal-representative
             :ref/deceased.info.id :deceased})))))




(defn routes []
  [["/case" {:post {:handler create-case
                    :coercion reitit.coercion.malli/coercion
                    :parameters {:body [:map
                                        [:personal-representative
                                         [:map
                                          [:title :string]
                                          [:forename :string]
                                          [:middlename {:optional true} :string]
                                          [:surname :string]
                                          [:dob [:re #"^\d{4}-\d{2}-\d{2}$"]]

                                          [:email :string]
                                          [:phone :string]

                                          [:flat {:optional true} :string]
                                          [:building {:optional true} :string]
                                          [:street-number {:optional true} :string]
                                          [:street1 :string]
                                          [:street2 {:optional true} :string]
                                          [:town :string]
                                          [:postcode :string]]]]}}}]

   ["/case/:case-id" {:patch {:handler update-case}
                      :get {:handler get-case}}]
   ["/bank/:case-id" {:patch {:handler add-bank
                              :coercion reitit.coercion.malli/coercion
                              :parameters {:body
                                           [:map
                                            [:bank-info
                                             [:map
                                              [:account
                                               [:vector
                                                [:map
                                                 [:sort-code string?]
                                                 [:account-number string?]
                                                 [:estimated-value string?]
                                                 [:joint-check {:optional true} boolean?]
                                                 [:joint-info {:optional true} string?]]]]
                                              [:bank-name string?]]]]}}}]

   ;:coercion reitit.coercion.malli/coercion}}]
   ;:parameters {:path {:case-id uuid?}
   ;             :body [:map
   ;                    [:deceased any?]]}}}]
   ["/cases" {:get {:handler get-cases}}]])

