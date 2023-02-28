(ns darbylaw.api.smart-search
  (:require [xtdb.api :as xt]
            [clojure.tools.logging :as log]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.smart-search.api :as ss-api]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.base64 :refer [decode-base64]]
            [darbylaw.api.util.pdf :as pdf]
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

(defn ->aml-data [{:keys [case-ref pr-info]}]
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
(defn ->doccheck-data [{:keys [case-ref pr-info]}]
  {:client_ref case-ref
   :sanction_region "gbr"
   :name {:title (:title pr-info)
          :first (:forename pr-info)
          :last (:surname pr-info)}
   :gender (:gender pr-info)
   :date_of_birth (:date-of-birth pr-info)
   ;; TODO: Same as above
   :address {:building (:street-number pr-info)
             :street_1 (:street1 pr-info)
             :town (:town pr-info)
             :postcode (:postcode pr-info)
             :country "gbr"}
   :issuing_country "gbr"
   :document_type ["driving_licence" "passport"]
   ;:scan_type "enhanced_selfie"
   ; For testing purposes use basic_selfie
   :scan_type "basic_selfie"
   :mobile_number (:phone pr-info)})

(defn ->fraudcheck-data [{:keys [case-ref pr-info]}]
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

(defn check-tx [case-id check-type data]
  (let [case-key (case check-type
                   :uk-aml :probate.identity-check.uk-aml/case
                   :fraudcheck :probate.identity-check.fraudcheck/case
                   :smartdoc :probate.identity-check.smartdoc/case)
        check-id {case-key case-id}
        check-data (merge data
                          check-id
                          {:xt/id check-id
                           :check-type check-type})]
    (tx-fns/set-values check-id check-data)))

(defn response->check-data [response]
  (-> response
      (get-in [:body :data :attributes])
      (select-keys [:result :status :ssid])))

(defn check [{:keys [xtdb-node user parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        {:keys [case-ref] :as check-data} (get-check-data xtdb-node case-id)

        ;; We perform each of the checks in their own try catches as they can independently fail
        ;; But even if they do fail we still want to save their results in one transaction so that the history will be updated correctly
        aml-data
        (try
          (-> check-data
              ->aml-data
              ss-api/aml
              response->check-data)
          (catch Exception e
            (log/error e "Failed UK AML API Call")
            nil))
        fraudcheck-data
        (try
          (when-let [aml-ssid (:ssid aml-data)]
            (-> check-data
                ->fraudcheck-data
                (->> (ss-api/fraudcheck "aml" aml-ssid))
                response->check-data))
          (catch Exception e
            (log/error e "Failed Fraudcheck API Call")
            nil))
        smartdoc-data
        (try
          (-> check-data
              ->doccheck-data
              ss-api/doccheck
              response->check-data)
          (catch Exception e
            (log/error e "Failed SmartDoc API Call")
            nil))

        aml-filename
        (try
          (let [export-resp (ss-api/export-pdf-base64 (:ssid aml-data))
                base64 (get-in export-resp [:body :data :attributes :base64])
                bytes (decode-base64 base64)

                document-id (random-uuid)
                filename (str case-ref ".identity.aml-report." document-id ".pdf")]
            (doc-store/store
              (str case-id "/" filename)
              bytes
              {:content-type "application/pdf"})
            filename)
          (catch Exception e
            (log/error e "Failed to store AML report")
            nil))

        failed? (or (nil? aml-data)
                    (nil? fraudcheck-data)
                    (nil? smartdoc-data)
                    (nil? aml-filename))]
    (xt-util/exec-tx xtdb-node
      (concat
        (->> [[:uk-aml (assoc aml-data :report aml-filename)]
              [:fraudcheck fraudcheck-data]
              [:smartdoc smartdoc-data]]
             ;; Remove failed checks
             (filter second)
             ;; Convert to transactions
             (map #(apply check-tx case-id %))
             (apply concat))
        (case-history/put-event
          {:event :identity.checks-added
           :case-id case-id
           :user user})))
    (if failed?
      {:status http/status-500-internal-server-error}
      {:status http/status-204-no-content})))

(defn override-checks [{:keys [xtdb-node user parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        new-result (get-in parameters [:query :new-result])]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value case-id [:override-identity-check]
                          (when new-result
                            (keyword new-result)))
        (case-history/put-event
          (merge
            {:event (if new-result
                      :identity.checks-overridden
                      :identity.checks-override-reset)
             :case-id case-id
             :user user}
            (when new-result
              {:result new-result})))))
    {:status http/status-204-no-content}))

(defn download [{:keys [xtdb-node parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        aml (xt/entity (xt/db xtdb-node)
                       {:probate.identity-check.uk-aml/case case-id})
        aml-report
        (when-let [filename (:report aml)]
          (doc-store/fetch (str case-id "/" filename)))

        smartdoc (xt/entity (xt/db xtdb-node)
                            {:probate.identity-check.smartdoc/case case-id})
        smartdoc-report
        (when-let [filename (:report smartdoc)]
          (doc-store/fetch (str case-id "/" filename)))]
    (if (or aml-report smartdoc-report)
      {:status http/status-200-ok
       :headers {"Content-Type" "application/pdf"}
       :body (with-open [out (java.io.ByteArrayOutputStream.)]
               (pdf/merge-pdfs
                 :input (->> [aml-report smartdoc-report]
                             (remove nil?))
                 :output out)
               (.toByteArray out))}
      {:status http/status-404-not-found
       :body {:error "No reports available"}})))


;; >> Routes

(defn routes []
  ["/case/:case-id/identity-checks"
   ["/run"
    {:post {:handler check
            :parameters {:path [:map [:case-id :uuid]]}}}]
   ["/override"
    {:post {:handler override-checks
            :parameters {:path [:map [:case-id :uuid]]
                         :query [:map [:new-result {:optional true} :string]]}}}]
   ["/download-pdf"
    {:get {:handler download
           :parameters {:path [:map [:case-id :uuid]]}}}]])
