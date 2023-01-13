(ns darbylaw.api.funeral
  (:require [clojure.tools.logging :as log]
            [reitit.coercion.malli]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.funeral.expense-store :as expense-store]
            [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]))

(defn upsert-funeral-account [{:keys [xtdb-node user parameters path-params multipart-params]}]
  (log/info "Upsert funeral account")
  (let [case-id (parse-uuid (:case-id path-params))
        query-params (:query parameters)
        {:keys [tempfile content-type]} (get multipart-params "file")
        account-info (cond-> query-params
                       tempfile (assoc (if (:paid query-params)
                                         :receipt-uploaded
                                         :invoice-uploaded)
                                       ;; TODO: Add by and when
                                       true))
        account-id {:probate.funeral-account/case case-id}]
    (when tempfile
      (let [s3-name (if (:paid account-info) "receipt" "invoice")]
        (try
          (doc-store/store
            (expense-store/s3-key case-id s3-name)
            tempfile
            {:content-type content-type})
          (finally
            (.delete tempfile)))))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values account-id
                           (merge account-info
                                  account-id
                                  {:xt/id account-id
                                   :type :probate.funeral-account}))
        (case-history/put-event {:event :updated.funeral-account
                                 :case-id case-id
                                 :user user})))
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


(defn upsert-other-expense [op {:keys [xtdb-node user parameters path-params multipart-params]}]
  (assert (contains? #{:add :update} op))
  (let [case-id (parse-uuid (:case-id path-params))
        expense-id (case op
                     :add (random-uuid)
                     :update (parse-uuid (:expense-id path-params)))
        query-params (:query parameters)
        {:keys [tempfile content-type]} (get multipart-params "file")
        expense-info (cond-> query-params
                       ;; TODO: Add by and when
                       tempfile (assoc :receipt-uploaded true))]
    (log/info (str (case :add "Add" :update "Update") " other funeral expense (" expense-id ")"))
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
        (tx-fns/set-values expense-id
                            (merge expense-info
                                   {:xt/id expense-id
                                    :type :probate.funeral-expense
                                    :probate.funeral-expense/case case-id}))
        (tx-fns/append-unique case-id [:funeral-expense] [expense-id])
        (case-history/put-event {:event (case 
                                          :add :added.other-expense
                                          :update :updated.other-expense)
                                 :case-id case-id
                                 :user user})))
    {:status 200
     :body {:id expense-id}}))

(comment
  (let [xtdb-node darbylaw.xtdb-node/xtdb-node
        case-id-str "34a6ff1a-c680-4b51-97f7-f14cebc1fc74"
        case-id (parse-uuid case-id-str)]
    (add-other-expense
      {:xtdb-node xtdb-node
       :user {:username "test"}
       :path-params {:case-id (str case-id)}
       :parameters
       {:query {:title "Test 1"
                :value "2345"
                :paid true
                :paid-by "Jim"}}})
    (upsert-funeral-account
      {:xtdb-node xtdb-node
       :user {:username "test"}
       :path-params {:case-id (str case-id)}
       :parameters
       {:query {:title "My Test Account"
                :value "2345"
                :paid true
                :paid-by "Jim"}}})
    #_(xt-util/exec-tx xtdb-node
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
   [:paid {:optional true} :boolean]
   [:paid-by {:optional true} :string]])

(def other-expense-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid {:optional true} :boolean]
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
    {:post {:handler (partial upsert-other-expense :add)
            :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id"
    {:put {:handler (partial upsert-other-expense :update)
           :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id/receipt"
    {:get {:handler (fn [{:keys [path-params] :as req}]
                      (let [expense-id (:expense-id path-params)
                            file-name (str expense-id "/receipt")
                            handler (get-funeral-file file-name)]
                        (handler req)))}}]])
