(ns darbylaw.api.smart-search
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.smart-search.api :as ss-api]))


;; >> Handlers

(defn get-uk-aml-data [xtdb-node case-id]
  (let [{:keys [case-ref pr-info]}
        (xt/pull (xt/db xtdb-node)
          '[(:reference {:as :case-ref})
            {(:probate.case/personal-representative 
              {:as :pr-info})
             [*]}]
          case-id)]
    {:client_ref case-ref
     :risk_level "high"
     :name {:title (:title pr-info)
            :first (:forename pr-info)
            :last (:surname pr-info)}
     :date_of_birth (:date-of-birth pr-info)
     :contacts {:mobile (:phone pr-info)}
     :addresses [{:building (:street-number pr-info)
                  :street_1 (:street1 pr-info)
                  :town (:town pr-info)
                  :postcode (:postcode pr-info)}]}))

(defn update-check [xtdb-node type case-id data]
  (let [check-id {:probate.identity-check/case case-id
                  :type type}
        check-data (merge data
                          check-id
                          {:xt/id check-id})]
    (xt-util/exec-tx xtdb-node
      ;; TODO: Add to case history
      (tx-fns/set-values check-id check-data))))

(defn check [{:keys [xtdb-node parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        data (get-uk-aml-data xtdb-node case-id)
        response (ss-api/uk-aml-check data)
        ssid (get-in response [:data :attributes :ssid])
        result (get-in response [:data :attributes :result])]
    (update-check xtdb-node :uk-aml case-id {:ssid ssid :result result})
    {:status 200
     :body {}}))

(comment
  (check {:xtdb-node darbylaw.xtdb-node/xtdb-node
          :parameters {:path {:case-id (parse-uuid "f0c7c353-1258-4d0e-964c-95e1debf755b")}}})

  (xt/pull (xt/db darbylaw.xtdb-node/xtdb-node)
           '[*]
           (parse-uuid "f0c7c353-1258-4d0e-964c-95e1debf755b")))



;; >> Routes

(defn routes []
  ["/case/:case-id/identity"
   {:post {:handler check
           :parameters {:path [:map [:case-id :uuid]]}}}])
