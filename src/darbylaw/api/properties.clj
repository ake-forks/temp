(ns darbylaw.api.properties
  (:require
    [clojure.edn :as edn]
    [clojure.string :as string]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.data :as data-util]
    [stencil.log :as log]
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

(defn get-idx [s]
  (second (reverse s)))

;{file-1
; {:filename grant.pdf, :content-type application/pdf, :tempfile #object[java.io.File 0x6f0f1ff2 /tmp/ring-multipart-5114832455941265515.tmp], :size 3420},
; file-2
; {:filename test pdf.pdf, :content-type application/pdf, :tempfile #object[java.io.File 0x61c8ec63 /tmp/ring-multipart-10057644423403095740.tmp], :size 4516}}

;(clojure.tools.logging/info "add-doc" (get files "file-1"))

(defn add-documents [xtdb-node user case-id property-id files]
  (let [reference (model/get-reference xtdb-node case-id)]
    (mapv
      (fn [entry]
        (let [data (second entry)
              extension (data-util/file-extension (:filename data))
              document-id (random-uuid)
              filename (str reference ".property." document-id extension)]
          (clojure.tools.logging/info filename)
          (doc-store/store-case-file case-id filename (:tempfile data))
          (xt-util/exec-tx-or-throw xtdb-node
            (concat
              [[::xt/put {:type :probate.property-doc
                          :xt/id filename
                          :probate.property-doc/case case-id
                          :probate.property-doc/property property-id
                          :uploaded-by (:username user)
                          :uploaded-at (xt-util/now)
                          :original-filename (:filename (second entry))}]]))))
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
    (clojure.tools.logging/info "property data" property-data)
    (clojure.tools.logging/info multipart-params)
    (clojure.tools.logging/info "file data" file-data)
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge property-data
                     {:xt/id property-id
                      :type :probate.property
                      :probate.property/case case-id})]]))
    (add-documents xtdb-node user case-id property-id file-data)
    {:status 204}))

(defn routes []
  ["/property/:case-id"
   ["/add-property"
    {:post {:handler add-property}}]])