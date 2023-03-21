(ns darbylaw.api.properties
  (:require
    [clojure.edn :as edn]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.data :as data-util]
    [xtdb.api :as xt]
    [darbylaw.api.util.model :as model]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.doc-store :as doc-store]
    [darbylaw.api.util.files :refer [with-delete]]))

(defn post-document [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        reference (model/get-reference xtdb-node case-id)
        postcode (get multipart-params "postcode")
        {:keys [tempfile]} (get multipart-params "file")
        orig-filename (get multipart-params "filename")
        extension (data-util/file-extension orig-filename)
        document-id (random-uuid)
        filename (str reference ".property." postcode "." document-id extension)]
    (assert (not (clojure.string/blank? reference)))
    (with-delete [tempfile tempfile]
      (doc-store/store (str case-id "/" filename) tempfile))
    (xt-util/exec-tx-or-throw xtdb-node
      [[::xt/put {:type :probate.property-doc
                  :xt/id filename
                  :probate.property-doc/case case-id
                  :uploaded-by (:username user)
                  :uploaded-at (xt-util/now)
                  :original-filename orig-filename}]])
    {:status 200
     :body filename}))

(defn add-documents [xtdb-node user case-id property-id files]
  (let [reference (model/get-reference xtdb-node case-id)]
    (mapv
      (fn [entry]
        (let [data (second entry)
              extension (data-util/file-extension (:filename data))
              document-id (random-uuid)
              filename (str reference ".property-doc." document-id extension)]
          (doc-store/store-case-file case-id filename (:tempfile data))
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              [[::xt/put {:type :probate.property-doc
                          :xt/id filename
                          :probate.property-doc/case case-id
                          :probate.property-doc/property property-id
                          :uploaded-by (:username user)
                          :uploaded-at (xt-util/now)
                          :original-filename (:filename (second entry))}]]
              (case-history/put-event2
                {:case-id case-id
                  :property-id property-id
                  :user user
                  :subject :probate.property.property-doc
                  :op :uploaded
                  :filename filename})))))
      files)))

(def property-keys
  [:address :valuation :joint-ownership? :joint-owner])

(defn add-property [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property-data
        (-> multipart-params
          (update-keys keyword)
          (select-keys property-keys)
          (update-vals edn/read-string))
        file-data (filter #(re-find #"file" (first %)) multipart-params)
        property-id (random-uuid)]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge property-data
                     {:xt/id property-id
                      :type :probate.property
                      :probate.property/case case-id})]]
        (case-history/put-event2 {:case-id case-id
                                  :property-id property-id
                                  :user user
                                  :subject :probate.property
                                  :op :created})))
    (add-documents xtdb-node user case-id property-id file-data)
    {:status 204}))

(defn routes []
  ["/property/:case-id"
   ["/add-property"
    {:post {:handler add-property}}]])