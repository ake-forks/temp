(ns darbylaw.api.services.textract
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.config :as config]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.xtdb-node :as xtdb-node]
            [mount.core :as mount]
            [medley.core :as medley]
            [xtdb.api :as xt])
  (:import (com.amazonaws.services.textract AmazonTextract AmazonTextractClientBuilder)
           (com.amazonaws.services.textract.model AnalyzeDocumentRequest Document S3Object)))

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
    (medley/index-by :type)))

(defn convenient-blocks [analyze-result]
  (->> analyze-result
    bean
    :blocks
    (map bean)
    (map #(assoc % :relationships-map (relationships-map %)))))

(defn get-words [id->block block]
  (-> block
    :relationships-map (get "CHILD") :ids
    (->>
      (map id->block)
      (filter (comp #{"WORD"} :blockType))
      (mapv :text))))

(defn get-value-block [id->block block]
  (-> block :relationships-map (get "VALUE") :ids first id->block))

(defn gather-key-value-words [id->block blocks]
  (->> blocks
    (filter key-block?)
    (map (fn [block]
           (assoc block
             :key-words (get-words id->block block)
             :value-words (get-words id->block (get-value-block id->block block)))))))

(def result-filename "textract_analyze_doc_result.serialized")
(def result-loaded
  (with-open [is (java.io.ObjectInputStream.
                   (clojure.java.io/input-stream result-filename))]
    (.readObject is)))

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

(defn process-analyze-result [result]
  (let [blocks (convenient-blocks result)
        id->block (medley/index-by :id blocks)
        kv-words (gather-key-value-words id->block blocks)]
    (->> kv-words
      (map #(select-keys % [:key-words
                            :value-words
                            :confidence])))))

(defn request-analyze [s3-key]
  (.analyzeDocument textract
    (doto (AnalyzeDocumentRequest.)
      (.setFeatureTypes ["FORMS"])
      (.setDocument (doto (Document.)
                      (.setS3Object (doto (S3Object.)
                                      (.setBucket bucket-name)
                                      (.setName s3-key))))))))

(defn analyze [s3-key]
  (let [etag (get-etag s3-key)]
    (if-let [cached (from-cache etag)]
      {:key-values cached
       :cache true}
      (let [result (request-analyze s3-key)
            key-values (process-analyze-result result)]
        (xt-util/exec-tx-or-throw xtdb-node/xtdb-node
          (cache-tx etag key-values))
        {:key-values key-values
         :cache false}))))

(comment
  (def object-name
    (str "5d994979-d002-402e-af70-8e7c454311c5"
         "/"
         "000100.death-certificate.ccdbf519-360b-4070-803f-159430fbacd9.pdf"))

  (xt/q (xt/db xtdb-node/xtdb-node) '{:find [(pull e [*])]
                                      :where [[e :type :textract-cache]]})
  (from-cache object-name)
  (analyze object-name)
  (get-etag object-name)
  (xt/submit-tx xtdb-node/xtdb-node
    (cache-tx (get-etag object-name) (process-analyze-result result-loaded)))

  (analyze (str "5d994979-d002-402e-af70-8e7c454311c5"
                "/"
                "000100.death-certificate.ccdbf519-360b-4070-803f-159430fbacd9.pdf"))

  (.analyzeDocument textract
    (doto (AnalyzeDocumentRequest.)
      (.setFeatureTypes ["FORMS"])
      (.setDocument (doto (Document.)
                      (.setS3Object (doto (S3Object.)
                                      (.setBucket bucket-name)
                                      (.setName object-name)))))))

  (def result *1)

  result

  (def result-filename "textract_analyze_doc_result.serialized")

  (with-open [os (java.io.ObjectOutputStream.
                   (clojure.java.io/output-stream result-filename))]
    (.writeObject os result))

  (def result-loaded
    (with-open [is (java.io.ObjectInputStream.
                     (clojure.java.io/input-stream result-filename))]
      (.readObject is)))


  (def blocks (convenient-blocks result-loaded))

  (def id->block
    (medley/index-by :id blocks))

  (-> blocks
    (->> (filter key-block?)
      (map (fn [block]
             (assoc block
               :key-words (get-words id->block block)
               :value-words (get-words id->block (get-value-block id->block block)))))
      (filter (fn [block]
                (= (first (:key-words block)) "1.")))
      first
      (get-value-block id->block))
    :relationships-map (get "CHILD") :ids
    (->> (map id->block)
      (map #(select-keys % [:blockType :text])))))
