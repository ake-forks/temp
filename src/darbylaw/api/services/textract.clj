(ns darbylaw.api.services.textract
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [darbylaw.config :as config]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.xtdb-node :as xtdb-node]
            [mount.core :as mount]
            [medley.core :as medley]
            [xtdb.api :as xt]
            [jsonista.core :as json]
            [darbylaw.api.util.files :refer [with-delete create-temp-file]])
  (:import (com.amazonaws.services.textract AmazonTextract AmazonTextractClientBuilder)
           (com.amazonaws.services.textract.model AnalyzeDocumentRequest AnalyzeDocumentResult Document QueriesConfig Query S3Object)
           (com.fasterxml.jackson.databind ObjectMapper)))

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

(defn cache-s3-key [etag]
  (str/join "/" ["textract-cache"
                 "v1"
                 (str etag ".AnalyzeDocumentResult.json")]))

(def jackson-object-mapper (ObjectMapper.))

(defn to-cache [etag ^AnalyzeDocumentResult result]
  (with-delete [temp-file (create-temp-file "textract" ".json")]
    ; clear fields which classes can't be extracted from JSON
    (doto result
      (.setSdkResponseMetadata nil)
      (.setSdkHttpMetadata nil))
    (json/write-value temp-file result jackson-object-mapper)
    (doc-store/store (cache-s3-key etag) temp-file)))

(defn from-cache ^AnalyzeDocumentResult [etag]
  (try
    (let [cached (doc-store/fetch (cache-s3-key etag))]
      (.readValue jackson-object-mapper cached AnalyzeDocumentResult))
    (catch Exception e
      (when-not (= (-> e ex-data :error) ::doc-store/not-found)
        (log/debug e "Exception on getting object from cache"))
      nil)))

(comment
  (to-cache "test" (AnalyzeDocumentResult.))
  (from-cache "test"))

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
  (let [etag (get-etag s3-key)
        cached-analyze-result (from-cache etag)
        analyze-result (or cached-analyze-result
                           (let [r (request-analyze s3-key my-queries)]
                             (to-cache etag r)
                             r))
        blocks-by-id (digest-blocks-by-id analyze-result)
        result {:key-values (process-form blocks-by-id)
                :queries (process-queries blocks-by-id)
                :lines (process-lines blocks-by-id)}]
    (-> result
      (assoc :cache (some? cached-analyze-result)))))

(comment
  (def s3-key
    (str "5d994979-d002-402e-af70-8e7c454311c5"
         "/"
         "000100.death-certificate.ccdbf519-360b-4070-803f-159430fbacd9.pdf"))

  (from-cache (get-etag s3-key))

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

  (json/write-value-as-string analyze-result jackson-object-mapper)

  (to-cache "test" analyze-result)
  (def analyze-result2 (from-cache "test"))
  (= analyze-result analyze-result2)

  (def blocks-by-id (digest-blocks-by-id analyze-result))
  (process-form blocks-by-id)
  (process-queries blocks-by-id)
  (process-lines blocks-by-id)

  (->> (vals blocks-by-id)
    (map :blockType)
    (distinct))

  (count blocks-by-id))
