(ns darbylaw.api.bank-notification
  (:require [clojure.java.io :as io]
            [stencil.api :as stencil]
            [xtdb.api :as xt]
            [camel-snake-kebab.core :as csk]
            [darbylaw.api.bank-list :as banks]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.case :as case-api]
            [darbylaw.api.pdf :as pdf]
            [darbylaw.api.util.xtdb :as xt-util])
  (:import (java.io InputStream File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def get-case--query
  {:find [(list 'pull 'case [:xt/id
                             :deceased.info
                             :bank-accounts])]
   :where '[[case :type :probate.case]
            [case :xt/id case-id]]
   :in '[case-id]})

(defn fetch-one [xt-results]
  (assert (= 1 (count xt-results))
    (str "Expected one result, got " (count xt-results)))
  (ffirst xt-results))

(defn keys-to-camel-case [m]
  (clojure.walk/postwalk
    #(if (map-entry? %)
       (let [[k v] %]
         (if (namespace k)
           %
           [(csk/->camelCase k :separator #"[.-]") v]))
       %)
    m))

(comment
  (keys-to-camel-case {:to.camel-case :ignore-value :b {:to-camel.case 2} :ns/k :ignore-qualified}))

(defn extract-bank [case-data bank-id]
  (let [bank-data (first (filter #(= (:id %) bank-id) (:bank-accounts case-data)))]
    (-> case-data
      (assoc :bank (-> bank-data
                     (assoc :name (banks/bank-label bank-id))))
      (dissoc :bank-accounts))))

(def change-bank-notification-status--txn
  '(fn [ctx case-id bank-id new-status]
     (let [case-data (xtdb.api/entity (xtdb.api/db ctx) case-id)]
       [[::xt/put (assoc-in case-data [:bank bank-id :notification-status] new-status)]])))

(defn change-bank-notification-status-txns [case-id user bank-id status]
  (-> [[::xt/put {:xt/id ::change-bank-notification-status
                  :xt/fn change-bank-notification-status--txn}]
       [::xt/fn ::change-bank-notification-status case-id bank-id status]]
    (case-api/put-event :case.bank.notification.status
      case-id
      user
      {:bank-id bank-id
       :bank-notification-status status})))

(defn case-template-data [bank-id case-data]
  (-> case-data
    (extract-bank bank-id)
    (keys-to-camel-case)))

(def bank-notification-template
  (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx")))

(defn ^InputStream render-docx [template-data]
  (stencil/render! bank-notification-template template-data
    :output :input-stream))

(defn create-temp-file [suffix]
  (-> (Files/createTempFile nil suffix (into-array FileAttribute nil))
    .toFile))

(defn render-docx [template-data file]
  (stencil/render! bank-notification-template template-data
    :output file
    :overwrite? true))

(defn s3-key [case-id bank-id suffix]
  (str case-id "/bank-notification/" (name bank-id) suffix))

(defn convert-to-pdf-and-store [case-id bank-id docx]
  (let [^File pdf (create-temp-file ".pdf")]
    (try
      (pdf/convert-file docx pdf)
      (doc-store/store (s3-key case-id bank-id ".docx") docx)
      (doc-store/store (s3-key case-id bank-id ".pdf") pdf)
      (finally
        (.delete pdf)))))

(defn start-notification [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        case-data (fetch-one
                    (xt/q (xt/db xtdb-node) get-case--query case-id))
        ^File docx (create-temp-file ".docx")]
    (try
      (render-docx (case-template-data bank-id case-data) docx)
      (convert-to-pdf-and-store case-id bank-id docx)
      (finally
        (.delete docx)))
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id user bank-id :started)))
    {:status 204}))

(defn cancel-notification [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id user bank-id :cancelled)))
    {:status 204}))

(defn mark-notification-sent [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id user bank-id :notification-letter-sent)))
    {:status 204}))

(defn mark-values-confirmed [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id user bank-id :values-confirmed)))
    {:status 204}))


(def docx-mime-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

(defn get-notification [doc-type {:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        input-stream (doc-store/fetch
                       (s3-key case-id bank-id (case doc-type
                                                 :docx ".docx"
                                                 :pdf ".pdf")))]
    {:status 200
     :headers {"Content-Type"
               (case doc-type
                 :docx docx-mime-type
                 :pdf "application/pdf")}
     :body input-stream}))

(defn set-custom-letter-uploaded--txns [case-id bank-id]
  (-> (xt-util/assoc-in--txns case-id
        [:bank bank-id :notification-letter-author]
        :unknown-user)
    (case-api/put-event :case.bank.notification.uploaded-custom-letter
      {:bank-id bank-id})))

(defn post-notification [{:keys [xtdb-node path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        {:keys [^File tempfile content-type]} (get multipart-params "file")]
    (try
      (assert (= content-type docx-mime-type))
      (convert-to-pdf-and-store case-id bank-id tempfile)
      (finally
        (.delete tempfile)))
    (xt-util/exec-txn xtdb-node
      (set-custom-letter-uploaded--txns case-id bank-id))
    {:status 204}))

(comment
  (-> last-request :multipart-params))

(defn routes []
  [["/case/:case-id/bank/:bank-id"
    ["/start-notification" {:post {:handler start-notification}}]
    ["/cancel-notification" {:post {:handler cancel-notification}}]
    ["/mark-notification-sent" {:post {:handler mark-notification-sent}}]
    ["/mark-values-confirmed" {:post {:handler mark-values-confirmed}}]
    ["/notification-docx" {:get {:handler (partial get-notification :docx)}
                           :post {:handler post-notification}}]
    ["/notification-pdf" {:get {:handler (partial get-notification :pdf)}}]]])

(comment
  (def all-case-data
    (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node)
      #uuid"041c820a-2e09-4b73-8452-0d0c3feb281b"))

  (:bank case-data)

  (xt-util/exec-txn darbylaw.xtdb-node/xtdb-node
    [[::xt/put (update all-case-data :bank dissoc :virgin-money)]])

  (def temp-file
    (render-docx
      (case-template-data :britannia-bereavement-team case-data))),)
