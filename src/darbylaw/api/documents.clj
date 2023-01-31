(ns darbylaw.api.documents
  (:require
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.doc-store :as doc-store]
    [xtdb.api :as xt]
    [darbylaw.api.util.files :refer [with-delete]]))

(def accepted-documents
  #{:death-certificate :will :grant-of-probate})

(defn get-reference [xtdb-node case-id]
  (-> (xt/pull (xt/db xtdb-node)
        [:reference] case-id)
    :reference))

(defn post-document [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        reference (get-reference xtdb-node case-id)
        {:keys [tempfile content-type]} (get multipart-params "file")
        _ (assert (= content-type "application/pdf"))
        orig-filename (get multipart-params "filename")
        document-name (keyword (:document-name path-params))
        document-id (random-uuid)
        filename (str reference "." (name document-name) "." document-id ".pdf")]
    (assert (accepted-documents document-name))
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
        (tx-fns/set-value case-id [document-name] filename)))
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
     :headers {"Content-Type" "application/pdf"}
     :body input-stream}))

(defn routes []
  ["/case/:case-id"
   ["/document/:document-name"
    {:post {:handler post-document}
     :get {:handler get-document}}]])
