(ns darbylaw.api.other
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.other.data :as data]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.files :refer [with-delete]]))


;; >> Handlers

(defn upload-documents [{:keys [xtdb-node user parameters multipart-params]}]
  (let [{:keys [case-id vehicle-id]} (:path parameters)
        files (->> multipart-params
                   vals
                   (filter (fn [{:keys [tempfile]}] (not (nil? tempfile)))))
        case-ref (-> (xt/pull (xt/db xtdb-node) [:reference] case-id)
                     :reference)]
    (doseq [{original-filename :filename
             :keys [tempfile content-type]}
            files]
      (let [document-id (random-uuid)
            filename (str case-ref ".vehicle." vehicle-id ".document." document-id)]
        (with-delete [tempfile tempfile]
          (doc-store/store
            (str case-id "/" filename)
            tempfile
            {:content-type content-type}))
        (xt-util/exec-tx xtdb-node
          (concat
            [[::xt/put {:xt/id document-id
                        :filename filename
                        :uploaded-by (:username user)
                        :uploaded-at (xt-util/now)
                        :original-filename original-filename}]]
            (tx-fns/append-unique vehicle-id [:documents] [document-id])
            (case-history/put-event2
              {:case-id case-id
               :user user
               :subject :probate.case.vehicle.document
               :op :added
               :vehicle-id vehicle-id
               :document-id document-id})))))
    {:status http/status-204-no-content}))

(defn download-document [{:keys [xtdb-node parameters]}]
  (let [{:keys [case-id document-id]} (:path parameters)
        filename (-> (xt/pull (xt/db xtdb-node) [:filename] document-id)
                     :filename)
        s3-file (doc-store/fetch-raw
                  (str case-id "/" filename))
        file-metadata (.getObjectMetadata s3-file)]
    {:status http/status-200-ok
     :headers {"Content-Type" (.getContentType file-metadata)}
     :body (.getObjectContent s3-file)}))

(defn delete-document [{:keys [xtdb-node user parameters]}]
  (let [{:keys [case-id vehicle-id document-id]} (:path parameters)]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/delete document-id]]
        (tx-fns/remove-unique vehicle-id [:documents] [document-id])
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.vehicle.document
           :op :deleted
           :document-id document-id})))
    {:status http/status-204-no-content}))

(defn extract-files
  [multipart-params]
  (->> multipart-params
       (filter (fn [[k _v]]
                 (re-matches #"^-file-.*" k)))
       (into {})))

(defn upsert [{:keys [xtdb-node user parameters multipart-params] :as request}]
  (let [{:keys [case-id vehicle-id]} (:path parameters)
        insert? (nil? vehicle-id)
        vehicle-id (if insert? (random-uuid) vehicle-id)
        vehicle-data (:multipart parameters)]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values vehicle-id 
                           (merge {:xt/id vehicle-id
                                   :type :probate.vehicle
                                   :probate.vehicle/case case-id}
                                  vehicle-data))
        (tx-fns/append-unique case-id [:other-assets] [vehicle-id])
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.vehicle
           :vehicle-id vehicle-id
           :op (if insert? :added :updated)})))
    (let [files (extract-files multipart-params)]
      (when-not (empty? files)
        (upload-documents
          (assoc request
                 :multipart-params files
                 :parameters (assoc-in parameters [:path :vehicle-id] vehicle-id)))))
    {:status http/status-200-ok
     :body {:id vehicle-id}}))

(comment
  (upsert
    {:xtdb-node darbylaw.xtdb-node/xtdb-node
     :user {:username "osm"}
     :parameters {:path {:case-id (parse-uuid "c68c5adc-e4f1-4159-a9b1-0ab1de98c85c")
                         :vehicle-id (parse-uuid "41fa2bbf-5650-4d7b-b46b-fd140aafcc44")}
                  :multipart {:registration-number "CUA 12345"
                              :description "Silver Ford Fiesta"
                              :estimated-value "123.12"}}}))



;; >> Routes

(defn routes []
  ["/case/:case-id"
   ["/other"
    {:post {:handler upsert
            :parameters {:path [:map [:case-id :uuid]]
                         :multipart data/schema}}}]
   ["/other/:vehicle-id"
    {:post {:handler upsert
            :parameters {:path [:map
                                [:case-id :uuid]
                                [:vehicle-id :uuid]]
                         :multipart data/schema}}}]
   ["/other/:vehicle-id"
    ["/document"
     {:post {:handler upload-documents
             :parameters {:path [:map
                                 [:case-id :uuid]
                                 [:vehicle-id :uuid]]}}}]
    ["/document/:document-id"
     {:get {:handler download-document
            :parameters {:path [:map
                                [:case-id :uuid]
                                [:vehicle-id :uuid]
                                [:document-id :uuid]]}}
      :delete {:handler delete-document
               :parameters {:path [:map
                                   [:case-id :uuid]
                                   [:vehicle-id :uuid]
                                   [:document-id :uuid]]}}}]]])
