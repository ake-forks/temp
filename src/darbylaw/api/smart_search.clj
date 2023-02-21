(ns darbylaw.api.smart-search
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.smart-search.api :as ss-api]
            [clojure.string :as str]))


;; >> Handlers

;; TODO: There's probably another function somewhere I can use
(defn get-check-data [xtdb-node case-id]
  (let [{:keys [case-ref pr-info]}
        (xt/pull (xt/db xtdb-node)
          '[(:reference {:as :case-ref})
            {(:probate.case/personal-representative 
              {:as :pr-info})
             [*]}]
          case-id)]
    {:case-ref case-ref
     :pr-info pr-info}))

(defn ->uk-aml-data [{:keys [case-ref pr-info]}]
  {:client_ref case-ref
   :risk_level "high"
   :name {:title (:title pr-info)
          :first (:forename pr-info)
          :last (:surname pr-info)}
   :date_of_birth (:date-of-birth pr-info)
   :contacts {:mobile (:phone pr-info)}
   ;; TODO: Match up requirements on PR info to schemas
   ;; TODO: Fix this so that it actually makes sense
   :addresses [{:building (:street-number pr-info)
                :street_1 (:street1 pr-info)
                :town (:town pr-info)
                :postcode (:postcode pr-info)}]})

;; TODO: Maybe pull out hard coded values into separate `base` def and merge?
(defn ->smart-doc-data [{:keys [case-ref pr-info]}]
  {:client_ref case-ref
   :sanction_region "gbr"
   :name {:title (:title pr-info)
          :first (:forename pr-info)
          :last (:surname pr-info)}
   :gender "male" ;; TODO: Add to PR info
   :date_of_birth (:date-of-birth pr-info)
   ;; TODO: Same as above
   :address {:building (:street-number pr-info)
             :street_1 (:street1 pr-info)
             :town (:town pr-info)
             :postcode (:postcode pr-info)
             :country "gbr"}
   :issuing_country "gbr"
   :document_type "driving_licence"
   ;:scan_type "enhanced_selfie"
   ; For testing purposes use basic_selfie
   :scan_type "basic_selfie"
   :mobile_number (:phone pr-info)})

(defn ->fraud-check-data [{:keys [case-ref pr-info]}]
  {:client_ref case-ref
   :sanction_region "gbr"
   :name {:title (:title pr-info)
          :first (:forename pr-info)
          :last (:surname pr-info)}
   :date_of_birth (:date-of-birth pr-info)
   :contacts {:mobile (:phone pr-info)}
   :address {:line_1 (str/join " " [(:street-number pr-info) (:street1 pr-info)])
             :city (:town pr-info)
             :postcode (:postcode pr-info)
             :country "gbr"}})

(defn update-check [type case-id data]
  (let [check-id {:probate.identity-check/case case-id
                  :type type}
        check-data (merge data
                          check-id
                          {:xt/id check-id})]
    ;; TODO: Add to case history
    (tx-fns/set-values check-id check-data)))

(defn check [{:keys [xtdb-node parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        check-data (get-check-data xtdb-node case-id)]
    ;; TODO: Split up into three separate calls that can each fail
    ;; If a check is made we want to save the result even if other checks fail
    (xt-util/exec-tx xtdb-node
      (let [aml-response
            (ss-api/uk-aml-check
              (->uk-aml-data check-data))
            smart-doc-response
            (ss-api/smart-doc-check
              (->smart-doc-data check-data))
            fraud-check-response
            (ss-api/fraud-check "aml" (get-in aml-response [:data :attributes :ssid])
              (->fraud-check-data check-data))]
        (apply concat
          (for [{:keys [type response]}
                [{:type :uk-aml
                  :response aml-response}
                 {:type :smart-doc
                  :response smart-doc-response}
                 {:type :fraud-check
                  :response fraud-check-response}]]
            (let [{:keys [result status ssid]} (get-in response [:data :attributes])]
              (update-check type case-id
                (cond-> {}
                  :always (assoc :ssid ssid)
                  result (assoc :result result)
                  status (assoc :status status))))))))
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
