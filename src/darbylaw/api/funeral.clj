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

(defn upsert-funeral-account [{:keys [xtdb-node user parameters path-params multipart-params]}]
  (log/info "Upsert funeral account")
  (let [case-id (parse-uuid (:case-id path-params))
        account-info (:query parameters)
        {:keys [tempfile content-type]} (get multipart-params "file")
        account-id {:probate.funeral-account/case case-id}
        file-tx
        (when tempfile
          (with-delete [tempfile tempfile]
            (let [document-name (if (:paid account-info) :receipt :invoice)
                  original-filename (get multipart-params "filename")
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
                (tx-fns/set-value account-id [document-name] filename)))))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values account-id
                           (merge account-info
                                  account-id
                                  {:xt/id account-id
                                   :type :probate.funeral-account}))
        file-tx
        (case-history/put-event {:event :updated.funeral-account
                                 :case-id case-id
                                 :user user})))
    {:status 200
     :body {:success? true}}))

(defn get-funeral-file [expense-id document-name]
  (fn [{:keys [xtdb-node path-params]}]
    (let [case-id (parse-uuid (:case-id path-params))
          filename (-> (xt/pull (xt/db xtdb-node)
                         [document-name]
                         expense-id)
                       (get document-name))
          s3-file (doc-store/fetch-raw
                    (expense-store/s3-key case-id filename))
          file-metadata (.getObjectMetadata s3-file)]
      {:status 200
       :headers {"Content-Type" (.getContentType file-metadata)}
       :body (.getObjectContent s3-file)})))

(defn get-funeral-file-old [file-name]
  (fn [{:keys [path-params]}]
    (let [case-id (parse-uuid (:case-id path-params))
          s3-file (doc-store/fetch-raw
                    (expense-store/s3-key case-id file-name))
          file-metadata (.getObjectMetadata s3-file)]
      {:status 200
       :headers {"Content-Type" (.getContentType file-metadata)}
       :body (.getObjectContent s3-file)})))

;; TODO: Refactor to combine with upsert-funeral-account
(defn upsert-other-expense [op {:keys [xtdb-node user parameters path-params multipart-params]}]
  (assert (contains? #{:add :update} op))
  (log/info "Upsert other funeral expense")
  (let [case-id (parse-uuid (:case-id path-params))
        expense-info (:query parameters)
        expense-id (case op
                     :add (random-uuid)
                     :update (parse-uuid (:expense-id path-params)))
        {:keys [tempfile content-type]} (get multipart-params "file")
        file-tx
        (when tempfile
          (with-delete [tempfile tempfile]
            (let [document-name :receipt
                  original-filename (get multipart-params "filename")
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
    (log/info (str (case :add "Add" :update "Update") " other funeral expense (" expense-id ")"))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values expense-id
                            (merge expense-info
                                   {:xt/id expense-id
                                    :type :probate.funeral-expense
                                    :probate.funeral-expense/case case-id}))
        file-tx
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
     {:get {:handler (fn [{:keys [path-params] :as req}]
                       (let [case-id (parse-uuid (:case-id path-params))
                             account-id {:probate.funeral-account/case case-id}
                             handler (get-funeral-file account-id :receipt)]
                         (handler req)))}}]
    ["/invoice"
     {:get {:handler (fn [{:keys [path-params] :as req}]
                       (let [case-id (parse-uuid (:case-id path-params))
                             account-id {:probate.funeral-account/case case-id}
                             handler (get-funeral-file account-id :invoice)]
                         (handler req)))}}]]
   ["/other"
    {:post {:handler (partial upsert-other-expense :add)
            :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id"
    {:put {:handler (partial upsert-other-expense :update)
           :parameters {:query other-expense-schema}}}]
   ["/other/:expense-id/receipt"
    {:get {:handler (fn [{:keys [path-params] :as req}]
                      (let [expense-id (parse-uuid (:expense-id path-params))
                            handler (get-funeral-file expense-id :receipt)]
                        (handler req)))}}]])
