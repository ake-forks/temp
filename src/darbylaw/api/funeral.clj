(ns darbylaw.api.funeral
  (:require [clojure.tools.logging :as log]
            [reitit.coercion.malli]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.funeral.expense-store :as expense-store]
            [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]))

(defn upsert-funeral-account [{:keys [xtdb-node parameters path-params multipart-params]}]
  (log/info "Upsert funeral account")
  (let [case-id (parse-uuid (:case-id path-params))
        query-params (:query parameters)
        {:keys [tempfile content-type]} (get multipart-params "file")
        account-info (cond-> query-params
                       tempfile (assoc (if (:paid? query-params)
                                         :receipt-uploaded
                                         :invoice-uploaded)
                                       ;; TODO: Add by and when
                                       true))]
    (when tempfile
      (let [s3-name (if (:paid? account-info) "receipt" "invoice")]
        (try 
          (doc-store/store
            (expense-store/s3-key case-id s3-name)
            tempfile
            {:content-type content-type})
          (finally
            (.delete tempfile)))))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/merge-value case-id
          [:funeral-account] account-info)))
    {:status 200
     :body {:success? true}}))

(defn get-funeral-file [file-name]
  (fn [{:keys [path-params]}]
    (let [case-id (parse-uuid (:case-id path-params))
          s3-file (doc-store/fetch-raw
                    (expense-store/s3-key case-id file-name))
          file-metadata (.getObjectMetadata s3-file)]
      {:status 200
       :headers {"Content-Type" (.getContentType file-metadata)}
       :body (.getObjectContent s3-file)})))

(defn add-other-expense [{:keys [xtdb-node parameters path-params multipart-params]}]
  (log/info "Add other funeral expense")
  (let [case-id (parse-uuid (:case-id path-params))
        query-params (:query parameters)
        expense-id (random-uuid)
        {:keys [tempfile content-type]} (get multipart-params "file")
        expense-info (cond-> query-params
                       ;; TODO: Add by and when
                       tempfile (assoc :receipt-uploaded true))]
    (println expense-info)
    (when tempfile
      (let [s3-name (str expense-id "/receipt")]
        (try 
          (doc-store/store
            (expense-store/s3-key case-id s3-name)
            tempfile
            {:content-type content-type})
          (finally
            (.delete tempfile)))))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value case-id
          [:funeral-expense expense-id] expense-info)))
    {:status 200
     :body {:id expense-id}}))

(defn update-other-expense [{:keys [xtdb-node parameters path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        expense-id (parse-uuid (:expense-id path-params))
        query-params (:query parameters)
        {:keys [tempfile content-type]} (get multipart-params "file")
        expense-info (cond-> query-params
                       ;; TODO: Add by and when
                       tempfile (assoc :receipt-uploaded true))]
    (log/info (str "Update other funeral expense (" expense-id ")"))
    (println (get multipart-params "file"))
    (println expense-info)
    (when tempfile
      (let [s3-name (str expense-id "/receipt")]
        (try 
          (doc-store/store
            (expense-store/s3-key case-id s3-name)
            tempfile
            {:content-type content-type})
          (finally
            (.delete tempfile)))))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value case-id
          [:funeral-expense expense-id] expense-info)))
    {:status 200
     :body {:id expense-id}}))

(comment
  (let [xtdb-node darbylaw.xtdb-node/xtdb-node
        case-id-str "aca8c621-e44b-4b75-9786-4297b89e72ce"
        case-id (parse-uuid case-id-str)
        expense-id (parse-uuid "3a941e79-04f8-4207-9913-74d64321f160")]
    #_
    (update-other-expense
      {:xtdb-node xtdb-node
       :path-params {:case-id (str case-id)
                     :expense-id (str expense-id)}
       :body-params {:title "My Test Account"
                     :value 2345
                     :paid? true
                     :paid-by "Jim"}})
    #_
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value case-id
          [:funeral-account] nil)))
    (xt/q (xt/db xtdb-node)
     {:find ['(pull case [*])]
      :where [['case :xt/id case-id]]})))

(def funeral-account-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid? {:optional true} :boolean]
   [:receipt {:optional true} :string]
   [:paid-by {:optional true} :string]
   [:invoice {:optional true} :string]])

(def other-expense-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid? {:optional true} :boolean]
   [:receipt {:optional true} :string]
   [:paid-by {:optional true} :string]])

(defn routes []
  ["/case/:case-id/funeral"
   ["/account"
    {:put {:handler upsert-funeral-account
           :parameters {:query funeral-account-schema}}}]
   ["/account"
    ["/receipt"
     {:get {:handler (get-funeral-file "receipt")}}]
    ["/invoice"
     {:get {:handler (get-funeral-file "invoice")}}]]
   ["/other"
    {:post {:handler add-other-expense
            :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id"
    {:put {:handler update-other-expense
           :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id/receipt"
    {:get {:handler (fn [{:keys [path-params] :as req}]
                      (let [expense-id (:expense-id path-params)
                            file-name (str expense-id "/receipt")
                            handler (get-funeral-file file-name)]
                        (handler req)))}}]])
