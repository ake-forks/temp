(ns darbylaw.api.smart-search.documents
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.files :refer [with-delete]]))

(defn upsert-document [{:keys [xtdb-node user parameters multipart-params]}]
  (let [{:keys [case-id document-id]} (:path parameters)
        document-id (if document-id document-id (random-uuid))

        case-ref (-> xtdb-node
                     xt/db
                     (xt/pull [:reference] case-id)
                     :reference)
        {original-filename :filename :keys [tempfile content-type]} (get multipart-params "file")
        filename (str case-ref ".identity.user-document." document-id)]
    (with-delete [tempfile tempfile]
      (doc-store/store
        (str case-id "/" filename)
        tempfile
        {:content-type content-type}))
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/put {:xt/id document-id
                    :type :probate.identity-check.document
                    :probate.identity-check.document/case case-id
                    :filename filename
                    :uploaded-by (:username user)
                    :uploaded-at (xt-util/now)
                    :original-filename original-filename}]]
        (tx-fns/append-unique case-id [:identity-user-docs] [document-id])
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.identity-check.document
           :op :added
           :document-id document-id})))
    {:status http/status-200-ok
     :body {:id document-id}}))

(defn download-document [{:keys [xtdb-node parameters]}]
  (let [{:keys [case-id document-id]} (:path parameters)
        filename (-> xtdb-node
                     xt/db
                     (xt/pull [:filename] document-id)
                     :filename)
        s3-file (doc-store/fetch-raw
                  (str case-id "/" filename))
        file-metadata (.getObjectMetadata s3-file)]
    {:status http/status-200-ok
     :headers {"Content-Type" (.getContentType file-metadata)}
     :body (.getObjectContent s3-file)}))

(defn delete-document [{:keys [xtdb-node user parameters]}]
  (let [{:keys [case-id document-id]} (:path parameters)]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/delete document-id]]
        (tx-fns/remove-unique case-id [:identity-user-docs] [document-id])
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.identity-check.document
           :op :deleted
           :document-id document-id})))
    {:status http/status-204-no-content}))

(defn routes []
  [["/document"
    {:post {:handler upsert-document
            :parameters {:path [:map [:case-id :uuid]]}}}]
   ["/document/:document-id"
    {:post {:handler upsert-document
            :parameters {:path [:map
                                [:case-id :uuid]
                                [:document-id :uuid]]}}
     :get {:handler download-document
           :parameters {:path [:map
                               [:case-id :uuid]
                               [:document-id :uuid]]}}
     :delete {:handler delete-document
              :parameters {:path [:map
                                  [:case-id :uuid]
                                  [:document-id :uuid]]}}}]])
