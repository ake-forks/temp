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

(comment
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"51127427-6ff1-4093-9929-c2c9990c796e")
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node) #uuid"162f1c25-ac28-45a9-9663-28e2accf11dc"))

(defn get-cases [{:keys [xtdb-node]}]
  (ring/response
    (->> (xt/q (xt/db xtdb-node)
           '{:find [(pull case [:xt/id])
                    (pull case [{:ref/personal-representative.info.id
                                 [:forename
                                  :surname
                                  :postcode]}])]
             :where [[case :type :probate.case]]})
      (map (fn [[case {pr-info :ref/personal-representative.info.id}]]
             (-> case
               (clojure.set/rename-keys {:xt/id :id})
               (assoc :personal-representative pr-info)))))))

(defn routes []
  [["/case" {:post {:handler create-case
                    :coercion reitit.coercion.malli/coercion
                    :parameters {:body [:map
                                        [:personal-representative
                                         [:map
                                          [:forename string?]
                                          [:surname string?]
                                          [:postcode string?]]]]}}}]
   ["/case/:case-id" {:patch {:handler update-case}}]
                              ;:coercion reitit.coercion.malli/coercion}}]
                              ;:parameters {:path {:case-id uuid?}
                              ;             :body [:map
                              ;                    [:deceased any?]]}}}]
   ["/cases" {:get {:handler get-cases}}]])
