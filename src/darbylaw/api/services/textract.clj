(ns darbylaw.api.services.textract
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.config :as config]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.xtdb-node :as xtdb-node]
            [mount.core :as mount]
            [medley.core :as medley]
            [xtdb.api :as xt])
  (:import (com.amazonaws.services.textract AmazonTextract AmazonTextractClientBuilder)
           (com.amazonaws.services.textract.model AnalyzeDocumentRequest Document QueriesConfig Query S3Object)))

(mount/defstate ^AmazonTextract textract
  :start (AmazonTextractClientBuilder/defaultClient))

(mount/defstate ^String bucket-name
  :start (get-in config/config [:doc-store :s3-bucket]))


(defn key-block? [block]
  (and (= "KEY_VALUE_SET" (:blockType block))
       (contains? (set (:entityTypes block)) "KEY")))

(defn relationships-map [block]
  (->> block
    :relationships
    (map bean)
    (medley/index-by :type)
    (medley/map-keys keyword)
    (medley/map-vals :ids)))

(defn digest-blocks-by-id [analyze-result]
  (->> analyze-result
    bean
    :blocks
    (map bean)
    (map #(assoc % :relationships-map (relationships-map %)))
    (medley/index-by :id)))

(defn get-words [blocks-by-id block]
  (-> block
    :relationships-map :CHILD
    (->>
      (map blocks-by-id)
      (filter (comp #{"WORD"} :blockType))
      (mapv :text))))

(defn get-value-block [blocks-by-id block]
  (-> block :relationships-map :VALUE first blocks-by-id))

(defn gather-key-value-words [blocks-by-id]
  (->> (vals blocks-by-id)
    (filter key-block?)
    (map (fn [block]
           (assoc block
             :key-words (get-words blocks-by-id block)
             :value-words (get-words blocks-by-id (get-value-block blocks-by-id block)))))))

(defn process-form [blocks-by-id]
  (let [kv-words (gather-key-value-words blocks-by-id)]
    (->> kv-words
      (map #(select-keys % [:key-words
                            :value-words
                            :confidence])))))

(defn process-queries [blocks-by-id]
    (->> (vals blocks-by-id)
      (filter #(= "QUERY" (:blockType %)))
      (map (fn [query-block]
             (let [answer-block (-> query-block
                                  :relationships-map
                                  :ANSWER
                                  first
                                  blocks-by-id)
                   query (bean (:query query-block))]
               (assoc query-block
                 :answer-text (:text answer-block)
                 :answer-confidence (:confidence answer-block)
                 :query-text (:text query)
                 :query-alias (keyword (:alias query))))))
      (map #(select-keys % [:answer-text
                            :answer-confidence
                            :query-text
                            :query-alias]))))

(defn process-lines [blocks-by-id]
  (->> (vals blocks-by-id)
    (filter #(= (:blockType %) "PAGE"))
    (sort-by :page)
    (mapcat (fn [page-block]
              (->> page-block
                :relationships-map :CHILD
                (map blocks-by-id)
                (filter #(= (:blockType %) "LINE"))
                (map #(select-keys % [:text
                                      :confidence])))))))

; Cache

(defn get-etag [s3-key]
  (some-> doc-store/s3
    (.getObjectMetadata bucket-name s3-key)
    (.getETag)))

(defn cache-tx [etag result]
  [[::xt/put {:xt/id {:textract-cache/etag etag}
              :type :textract-cache
              :result result
              :modified-at (xt-util/now)}]])

(defn from-cache [etag]
  (-> (xt/entity (xt/db xtdb-node/xtdb-node)
        {:textract-cache/etag etag})
    :result))

(def my-queries
  {:occupation "What is the occupation?"
   :address "What is the address in the occupation and usual address box?"
   :certified-by "Who certified the death?"})

(defn request-analyze [s3-key queries]
  (.analyzeDocument textract
    (doto (AnalyzeDocumentRequest.)
      (.setFeatureTypes ["FORMS"
                         "QUERIES"])
      (.setDocument (doto (Document.)
                      (.setS3Object (doto (S3Object.)
                                      (.setBucket bucket-name)
                                      (.setName s3-key)))))
      (.setQueriesConfig (doto (QueriesConfig.)
                           (.setQueries
                             (for [[alias text] queries]
                               (doto (Query.)
                                 (.setAlias (name alias))
                                 (.setText text)))))))))

(defn analyze [s3-key]
  (let [etag (get-etag s3-key)]
    (if-let [result (from-cache etag)]
      (-> result
        (assoc :cache true))
      (let [analyze-result (request-analyze s3-key my-queries)
            blocks-by-id (digest-blocks-by-id analyze-result)
            result {:key-values (process-form blocks-by-id)
                    :queries (process-queries blocks-by-id)
                    :lines (process-lines blocks-by-id)}]
        (xt-util/exec-tx-or-throw xtdb-node/xtdb-node
          (cache-tx etag result))
        (-> result
          (assoc :cache false))))))

(comment
  (def s3-key
    (str "5d994979-d002-402e-af70-8e7c454311c5"
         "/"
         "000100.death-certificate.ccdbf519-360b-4070-803f-159430fbacd9.pdf"))

  (xt/q (xt/db xtdb-node/xtdb-node) '{:find [e]
                                      :where [[e :type :textract-cache]]})

  (def analyze-result (request-analyze s3-key my-queries))
  (def result-filename "textract_analyze_doc_result.serialized")

  (with-open [os (java.io.ObjectOutputStream.
                   (clojure.java.io/output-stream result-filename))]
    (.writeObject os analyze-result))

  (def analyze-result
    (with-open [is (java.io.ObjectInputStream.
                     (clojure.java.io/input-stream result-filename))]
      (.readObject is)))

  (def blocks-by-id (digest-blocks-by-id analyze-result))
  (process-form blocks-by-id)
  (process-queries blocks-by-id)
  (process-lines blocks-by-id)

  (->> (vals blocks-by-id)
    (map :blockType)
    (distinct))

  (count blocks-by-id)

  (->> (vals blocks-by-id)
    (filter #(= (:blockType %) "PAGE"))
    (sort-by :page)
    (mapcat (fn [page-block]
              (->> page-block
                :relationships-map :CHILD
                (map blocks-by-id)
                (filter #(= (:blockType %) "LINE"))
                (map #(select-keys % [:text
                                      :confidence])))))))

(defn delete-cache []
  (let [xt-node xtdb-node/xtdb-node
        ids (xt/q (xt/db xt-node)
              '{:find [e]
                :where [[e :type :textract-cache]]})]
    (xt-util/exec-tx-or-throw xt-node
      (for [[id] ids]
        [::xt/delete id]))))

(comment
  (delete-cache))