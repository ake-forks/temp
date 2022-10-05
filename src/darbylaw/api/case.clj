(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]))

(defn create-case [{:keys [xtdb-node body-params]}]
  (let [pr-info-id (random-uuid)
        pr-info (get body-params :personal-representative)]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        [[::xt/put {:type :probate.case
                    :xt/id (random-uuid)
                    :ref/personal-representative.info.id pr-info-id}]
         [::xt/put (merge
                     pr-info
                     {:type :probate.personal-representative.info
                      :xt/id pr-info-id})]])))
  {:status 204})

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
   ["/cases" {:get {:handler get-cases}}]])
