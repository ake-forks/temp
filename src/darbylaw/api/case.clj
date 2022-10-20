(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]))

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

(defn format-bank-info [{:keys [bank-name account]}]
  {(keyword (str (rand-int 100))) {:bank-name bank-name
                                   :accounts account}})

;(merge e {:banks (merge bank1 bank2)})

(def mergefield__txn-fn
  '(fn [ctx eid m]
     (let [e (xtdb.api/entity (xtdb.api/db ctx) eid)
           existing-banks (:banks e)]
       [[::xt/put (merge e {:banks (merge existing-banks m)})]])))



(defn add-bank [{:keys [xtdb-node path-params body-params]}]
  (let [bank-info (get body-params :bank-info)
        case-id (parse-uuid (:case-id path-params))]
    (when bank-info
      (xt/await-tx xtdb-node
        (xt/submit-tx xtdb-node
          [[::xt/put {:xt/id ::mergefield
                      :xt/fn mergefield__txn-fn}]
           [::xt/fn ::mergefield case-id (format-bank-info bank-info)]]))
      {:status 200
       :body {:id case-id :data (format-bank-info bank-info)}})))

(comment
  (let [node darbylaw.xtdb-node/xtdb-node]
    (xt/await-tx node
      (xt/submit-tx node
        [[::xt/put {:xt/id 12345 :name "hello"}]])))

  (let [node darbylaw.xtdb-node/xtdb-node]
    (xt/entity (xt/db node) 12345))

  (add-bank {:xtdb-node darbylaw.xtdb-node/xtdb-node
             :path-params {:case-id "23362950-f80d-48f0-8851-75299b9176ec"}
             :body-params {:bank-info {:bank-name "Royal Bank of Scotland Group"
                                       :account [{:sort-code 1 :account-number 11 :estimated-value 1}]}}}

    (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"a7f27974-49d5-43c7-ac3c-b650291b9bc4")
    ((xt/await-tx darbylaw.xtdb-node/xtdb-node
       (xt/submit-tx darbylaw.xtdb-node/xtdb-node)
       [[::xt/put {:xt/id 123
                   :name "hi"}]])))


  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull case [:xt/id
                         :bank-name
                         [:account-number
                          :sort-code]
                         {:ref/personal-representative.info.id
                          [:forename
                           :surname
                           :postcode]}])]
      :where [[case :type :probate.case]
              [case :bank _]]})
  (:banks (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node)
            #uuid"0a820791-1e83-4192-8854-0df8507eefff")))


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
      (clojure.set/rename-keys (ffirst results)
        {:xt/id :id
         :ref/personal-representative.info.id :personal-representative
         :ref/deceased.info.id :deceased}))))


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
