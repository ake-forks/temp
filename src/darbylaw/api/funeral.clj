(ns darbylaw.api.funeral
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [reitit.coercion.malli]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.funeral.expense-store :as expense-store]
            [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.files :refer [with-delete]]))

(defn wrap-funeral-account
  "Extracts information needed to add/update the funeral account.
  See `upsert-funeral-expense`."
  [handler]
  (fn [{:keys [parameters]
        :as request}]
    (log/info "Upsert funeral account")
    (let [case-id (get-in parameters [:path :case-id])
          account-id {:probate.funeral-account/case case-id}
          account-info (merge (:query parameters)
                              account-id)

          request' (assoc request
                          :xt-type :probate.funeral-account
                          :event :updated.funeral-account
                          :expense-id account-id
                          :expense-info account-info)]
      (handler request'))))

(defn wrap-other-expense
  "Extracts information needed to add/update a funeral expense.
  See `upsert-funeral-expense`."
  [op handler]
  (assert (contains? #{:add :update} op))
  (fn [{:keys [parameters]
        :as request}]
    (let [case-id (get-in parameters [:path :case-id])
          expense-id (case op
                       :add (random-uuid)
                       :update (get-in parameters [:path :expense-id]))
          _ (log/info (str (case :add "Add" :update "Update") " other funeral expense (" expense-id ")"))
          expense-info (merge (:query parameters)
                              {:probate.funeral-expense/case case-id})
          
          request' (assoc request
                          :xt-type :probate.funeral-expense
                          :append-path [:funeral-expense]
                          :event (case op
                                   :add :added.other-expense
                                   :update :updated.other-expense)
                          :expense-id expense-id
                          :expense-info expense-info)]
      (handler request'))))

(defn upsert-funeral-expense
  "Insert or update a funeral expense.
  
  Arguments:
  xt-type      - The xt/type of the expense.
  event        - The event to add to the case history.
  expense-id   - The xt/id of the expense.
  expense-info - The expense info to insert/update.
  append-path  - (optional) The path to append the expense id to.
                 If not specified, the expense id will not be appended to any path.
                 This is useful for funeral accounts which are singletons.
  file-uploads - See wrap-uploaded-files."
  [{:keys [xtdb-node user parameters file-uploads
           xt-type event
           expense-id expense-info append-path]}]
  (let [case-id (get-in parameters [:path :case-id])
        file-tx
        (for [[document-name {original-filename :filename :keys [tempfile content-type]}]
              file-uploads]
          (with-delete [tempfile tempfile]
            (let [case-ref (-> (xt/pull (xt/db xtdb-node) [:reference] case-id)
                               :reference)
                  document-id (random-uuid)
                  filename (str case-ref "." (name document-name) "." document-id)]
              (doc-store/store
                (expense-store/s3-key case-id filename)
                tempfile
                {:content-type content-type})
              (concat
                [[::xt/put {:type :probate.case-doc
                            :xt/id filename
                            :document-name document-name
                            :probate.case-doc/case case-id
                            :uploaded-by (:username user)
                            :uploaded-at (xt-util/now)
                            :original-filename original-filename}]]
                (tx-fns/set-value expense-id [document-name] filename)))))
        file-tx (->> file-tx
                     doall ; To ensure that file uploads happen before the xtdb transaction
                     (apply concat))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values expense-id
                           (merge expense-info
                                  {:xt/id expense-id
                                   :type xt-type}))
        file-tx
        (when append-path
          (tx-fns/append-unique case-id append-path [expense-id]))
        (case-history/put-event {:event event
                                 :case-id case-id
                                 :user user})))
    {:status 200
     :body {:id expense-id}}))

(defn get-funeral-file [xtdb-node case-id expense-id document-name]
  (let [filename (-> (xt/pull (xt/db xtdb-node)
                       [document-name]
                       expense-id)
                     (get document-name))
        s3-file (doc-store/fetch-raw
                  (expense-store/s3-key case-id filename))
        file-metadata (.getObjectMetadata s3-file)]
    {:status 200
     :headers {"Content-Type" (.getContentType file-metadata)}
     :body (.getObjectContent s3-file)}))

(defn get-funeral-account-file [document-name {:keys [xtdb-node parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        account-id {:probate.funeral-account/case case-id}]
    (get-funeral-file xtdb-node case-id account-id document-name)))

(defn get-other-expense-file [{:keys [xtdb-node parameters]}]
  (let [case-id (get-in parameters [:path :case-id])
        document-name :receipt
        expense-id (get-in parameters [:path :expense-id])]
    (get-funeral-file xtdb-node case-id expense-id document-name)))

(def expense-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid {:optional true} :boolean]
   [:paid-by {:optional true} :string]])

(defn wrap-uploaded-files
  "Looks for files in `:multipart-params` and puts into `:file-uploads`
  Also transforms keys to be keywords."
  [handler]
  (fn [{:keys [multipart-params] :as request}]
    (let [files (->> multipart-params
                     ;; Keep only files
                     (filter (fn [[_ v]] (:tempfile v)))
                     (map (fn [[k v]] [(keyword k) v]))
                     (into {}))]
      (handler (assoc request :file-uploads files)))))

(defn wrap-allowed-files
  "Given a set of allowed keys, checks that only those keys are present in
  `:file-uploads`."
  [allowed-keys handler]
  (fn [{:keys [file-uploads] :as request}]
    (let [uploaded-keys (->> file-uploads keys (into #{}))
          disallowed-keys (set/difference uploaded-keys allowed-keys)]
      (if (empty? disallowed-keys)
        (handler request)
        (do (log/warn (str "Disallowed keys found: " disallowed-keys))
          {:status 400
           :body {:errors {:file-uploads (str "Only the following files are allowed: " allowed-keys)}}})))))

(defn routes []
  ["/case/:case-id/funeral"
   ["/account"
    {:put {:handler upsert-funeral-expense
           :middleware [wrap-uploaded-files
                        (partial wrap-allowed-files #{:receipt :invoice})
                        wrap-funeral-account]
           :parameters {:query (->> expense-schema)
                        :path [:map [:case-id :uuid]]}}}]
   ["/account"
    ["/receipt"
     {:get {:handler (partial get-funeral-account-file :receipt)
            :parameters {:path [:map [:case-id :uuid]]}}}]
    ["/invoice"
     {:get {:handler (partial get-funeral-account-file :invoice)
            :parameters {:path [:map [:case-id :uuid]]}}}]]
   ["/other"
    {:post {:handler upsert-funeral-expense
            :middleware [wrap-uploaded-files
                         (partial wrap-allowed-files #{:receipt})
                         (partial wrap-other-expense :add)]
            :parameters {:query expense-schema
                         :path [:map [:case-id :uuid]]}}}]
   ["/other/:expense-id"
    {:put {:handler upsert-funeral-expense
           :middleware [wrap-uploaded-files
                        (partial wrap-allowed-files #{:receipt})
                        (partial wrap-other-expense :update)]
           :parameters {:query expense-schema
                        :path [:map [:case-id :uuid]
                                    [:expense-id :uuid]]}}}]
   ["/other/:expense-id/receipt"
    {:get {:handler get-other-expense-file
           :parameters {:path [:map [:case-id :uuid]
                                    [:expense-id :uuid]]}}}]])
