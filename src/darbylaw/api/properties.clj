(ns darbylaw.api.properties
  (:require
    [clojure.edn :as edn]
    [clojure.tools.logging :as logger]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.tx-fns :as tx-fns]

    [xtdb.api :as xt]
    [darbylaw.api.util.model :as model]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.doc-store :as doc-store]))

(def property-keys
  [:address :valuation :joint-ownership? :joint-owner :insured? :estimated-value?])

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
                  :filename filename})))
          filename))
      files)))

(defn upload-document [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property-id (parse-uuid (:property-id path-params))
        filenames (add-documents xtdb-node user case-id property-id multipart-params)]
    (logger/info (first filenames))
    (case-history/put-event2
      {:case-id case-id
       :property-id property-id
       :user user
       :subject :probate.property.property-doc
       :op :uploaded
       :filename (first filenames)})
    {:status 204}))

(defn get-document [{:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        filename (:filename path-params)
        input-stream (doc-store/fetch
                       (str case-id "/" filename))]
    {:status 200
     :body input-stream}))

(defn remove-document [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        filename (:filename path-params)
        property-id (parse-uuid (:property-id body-params))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values filename
          {:probate.property-doc/property ""
           :probate.property-doc-removed/property property-id})
        (case-history/put-event2
          {:case-id case-id
           :property-id property-id
           :user user
           :subject :probate.property.property-doc
           :op :deleted
           :filename filename})))
    {:status 204}))


(defn add-property [op {:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property-data
        (-> multipart-params
          (update-keys keyword)
          (select-keys property-keys)
          (update-vals edn/read-string))
        file-data (filter #(re-find #"file" (first %)) multipart-params)
        property-id (case op
                      :new (random-uuid)
                      :update (parse-uuid (:property-id path-params)))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge property-data
                     {:xt/id property-id
                      :type :probate.property
                      :owned? true
                      :probate.property/case case-id})]]
        (case-history/put-event2 {:case-id case-id
                                  :property-id property-id
                                  :user user
                                  :subject :probate.property
                                  :op :created})))
    (add-documents xtdb-node user case-id property-id file-data)
    {:status 204}))

(defn edit-property [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property-id (parse-uuid (:property-id path-params))
        property-data body-params]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        (tx-fns/set-values property-id property-data)
        (case-history/put-event2 {:case-id case-id
                                  :property-id property-id
                                  :user user
                                  :subject :probate.property
                                  :op :updated}))))
  {:status 204})

(defn remove-property [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property-id (parse-uuid (:property-id path-params))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/delete property-id]]
        (case-history/put-event2 {:case-id case-id
                                  :property-id property-id
                                  :user user
                                  :subject :probate.property
                                  :op :deleted})))
    {:status 204}))

(defn routes []
  ["/property/:case-id"
   ["/add-property"
    {:post {:handler (partial add-property :new)}}]
   ["/update-property/:property-id"
    {:post {:handler (partial add-property :edit)}}]
   ["/edit-property/:property-id"
    {:post {:handler edit-property}}]
   ["/remove-property/:property-id"
    {:post {:handler remove-property}}]
   ["/post-document/:property-id"
     {:post {:handler upload-document}}]
   ["/get-document/:filename"
    {:get {:handler get-document}}]
   ["/remove-document/:filename"
    {:post {:handler remove-document}}]])