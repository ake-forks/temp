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
        [[::xt/put (merge pr-info
                     {:type :probate.personal-representative.info
                      :xt/id pr-info-id})]
         [::xt/put {:type :probate.case
                    :xt/id (random-uuid)
                    :ref/personal-representative.info.id pr-info-id}]])))
  {:status 204})

(defn get-cases [{:keys [xtdb-node]}]
  (ring/response
    (->> (xt/q (xt/db xtdb-node)
           '{:find [(pull case [*]) (pull pr-info [*])]
             :where [[case :type :probate.case]
                     ; TODO: Do a left join here. This is an inner join.
                     [case :ref/personal-representative.info.id pr-info-id]
                     [pr-info :xt/id pr-info-id]]})
      (map (fn [[case pr-info]]
             (-> case
               (assoc :id (:xt/id case)
                      :personal-representative (-> pr-info
                                                 (dissoc :type
                                                         :xt/id)))
               (dissoc :type
                       :xt/id
                       :ref/personal-representative.info.id)))))))

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
