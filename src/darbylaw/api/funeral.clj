(ns darbylaw.api.funeral
  (:require [clojure.tools.logging :as log]
            [reitit.coercion.malli]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.funeral.expense-store :as expense-store]
            [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.files :refer [with-delete]]))

(defn wrap-funeral-account [handler]
  (fn [{:keys [path-params parameters]
        :as request}]
    (log/info "Upsert funeral account")
    (let [case-id (parse-uuid (:case-id path-params))
          account-id {:probate.funeral-account/case case-id}
          account-info (merge (:query parameters)
                              account-id)
          document-name (if (:paid account-info) :receipt :invoice)

          request' (assoc request
                          :case-id case-id
                          :xt-type :probate.funeral-account
                          :event :updated.funeral-account
                          :expense-id account-id
                          :expense-info account-info
                          :document-name document-name)
          response (handler request')]
      (assoc-in response [:body :success] true))))

(defn wrap-other-expense [op handler]
  (assert (contains? #{:add :update} op))
  (fn [{:keys [path-params parameters]
        :as request}]
    (let [case-id (parse-uuid (:case-id path-params))
          expense-id (case op
                       :add (random-uuid)
                       :update (parse-uuid (:expense-id path-params)))
          _ (log/info (str (case :add "Add" :update "Update") " other funeral expense (" expense-id ")"))
          expense-info (merge (:query parameters)
                              {:probate.funeral-expense/case case-id})
          document-name :receipt
          event (case op
                  :add :added.other-expense
                  :update :updated.other-expense)
          
          request' (assoc request
                          :case-id case-id
                          :xt-type :probate.funeral-expense
                          :other-tx (tx-fns/append-unique case-id [:funeral-expense] [expense-id])
                          :event event
                          :expense-id expense-id
                          :expense-info expense-info
                          :document-name document-name)
          response (handler request')]
      (assoc-in response [:body :id] expense-id))))

(defn upsert-funeral-expense [{:keys [xtdb-node user multipart-params
                                      xt-type event other-tx
                                      case-id
                                      expense-id expense-info
                                      document-name]}]
  (let [{:keys [tempfile content-type]} (get multipart-params "file")
        file-tx
        (when tempfile
          (with-delete [tempfile tempfile]
            (let [original-filename (get multipart-params "filename")
                  case-ref (-> (xt/pull (xt/db xtdb-node) [:reference] case-id)
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
                (tx-fns/set-value expense-id [document-name] filename)))))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values expense-id
                           (merge expense-info
                                  {:xt/id expense-id
                                   :type xt-type}))
        file-tx
        other-tx
        (case-history/put-event {:event event
                                 :case-id case-id
                                 :user user})))
    {:status 200}))

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

(defn get-funeral-account-file [document-name {:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        account-id {:probate.funeral-account/case case-id}]
    (get-funeral-file xtdb-node case-id account-id document-name)))

(defn get-other-expense-file [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        document-name :receipt
        expense-id (parse-uuid (:expense-id path-params))]
    (get-funeral-file xtdb-node case-id expense-id document-name)))

(def expense-schema
  [:map
   [:title :string]
   [:value :string]
   [:paid {:optional true} :boolean]
   [:paid-by {:optional true} :string]])

(defn routes []
  ["/case/:case-id/funeral"
   ["/account"
    {:put {:handler (wrap-funeral-account
                      upsert-funeral-expense)
           :parameters {:query expense-schema}}}]
   ["/account"
    ["/receipt"
     {:get {:handler (partial get-funeral-account-file :receipt)}}]
    ["/invoice"
     {:get {:handler (partial get-funeral-account-file :invoice)}}]]
   ["/other"
    {:post {:handler (wrap-other-expense :add
                       upsert-funeral-expense)
            :parameters {:query expense-schema}}}]
   ["/other/:expense-id"
    {:put {:handler (wrap-other-expense :update
                      upsert-funeral-expense)
           :parameters {:query expense-schema}}}]
   ["/other/:expense-id/receipt"
    {:get {:handler get-other-expense-file}}]])
