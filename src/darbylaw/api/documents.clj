(ns darbylaw.api.documents
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.model :as model]
    [darbylaw.doc-store :as doc-store]
    [xtdb.api :as xt]
    [darbylaw.api.util.files :refer [with-delete]]
    [darbylaw.api.services.textract :as textract]))

(def accepted-documents
  #{:death-certificate :will :grant-of-probate})

(def accepted-filetypes
  #{".pdf" ".png" ".jpeg" ".jpg" ".gif"})

(defn document-present? [xtdb-node case-id document-name]
  (contains?
    (xt/pull (xt/db xtdb-node) (vec accepted-documents) case-id)
    document-name))

(defn post-document [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        reference (model/get-reference xtdb-node case-id)
        {:keys [tempfile]} (get multipart-params "file")
        orig-filename (get multipart-params "filename")
        extension (data-util/file-extension orig-filename)
        document-name (keyword (:document-name path-params))
        document-id (random-uuid)
        filename (str reference "." (name document-name) "." document-id extension)]
    (assert (accepted-documents document-name))
    (assert (accepted-filetypes extension))
    (assert (not (clojure.string/blank? reference)))
    (with-delete [tempfile tempfile]
      (doc-store/store (str case-id "/" filename) tempfile))
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put {:type :probate.case-doc
                    :xt/id filename
                    :document-name document-name
                    :probate.case-doc/case case-id
                    :uploaded-by (:username user)
                    :uploaded-at (xt-util/now)
                    :original-filename orig-filename}]]
        (tx-fns/set-value case-id [document-name] filename)
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.document
           :op (if (document-present? xtdb-node case-id document-name)
                 :replaced
                 :uploaded)
           :document-name document-name
           :document-id filename})))
    {:status 204}))

(defn get-document-id [xtdb-node case-id document-name]
  (-> (xt/pull (xt/db xtdb-node)
        [document-name] case-id)
    (get document-name)))

(defn get-document [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        document-name (keyword (:document-name path-params))
        filename (get-document-id xtdb-node case-id document-name)
        input-stream (doc-store/fetch
                       (str case-id "/" filename))]
    (assert (accepted-documents document-name))
    {:status 200
     :body input-stream}))

(defn analyze-death-certificate [{:keys [path-params xtdb-node]}]
  (let [case-id (parse-uuid (:case-id path-params))
        doc-name (keyword (:document-name path-params))
        _ (assert doc-name :death-certificate)
        doc-id (get-document-id xtdb-node case-id doc-name)
        s3-key (doc-store/s3-key case-id doc-id)
        analyze-result (textract/analyze s3-key)]
    {:status http/status-200-ok
     :body analyze-result}))

(defn routes []
  ["/case/:case-id"
   ["/document/:document-name"
    {:post {:handler post-document}
     :get {:handler get-document}}]
   ["/document/:document-name/analyze"
    {:get {:handler analyze-death-certificate}}]])
