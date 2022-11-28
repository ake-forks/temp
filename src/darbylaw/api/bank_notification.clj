(ns darbylaw.api.bank-notification
  (:require [clojure.java.io :as io]
            [ring.util.io :as ring-io]
            [stencil.api :as stencil]
            [xtdb.api :as xt]
            [camel-snake-kebab.core :as csk]
            [darbylaw.api.bank-list :as banks]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.case :as case-api]
            [darbylaw.api.pdf :as pdf])
  (:import (java.io InputStream)))

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

(defn change-bank-notification-status-txns [case-id bank-id status]
  (-> [[::xt/put {:xt/id ::change-bank-notification-status
                  :xt/fn change-bank-notification-status--txn}]
       [::xt/fn ::change-bank-notification-status case-id bank-id status]]
    (case-api/put-event :case.bank.notification.status
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

(defn bank-notification-s3-key [case-id bank-id]
  (str case-id "/bank-notification/" (name bank-id) ".pdf"))

(defn start-notification [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        case-data (fetch-one
                    (xt/q (xt/db xtdb-node) get-case--query case-id))]
    (with-open [rendered-docx (render-docx (case-template-data bank-id case-data))]
      (let [rendered-pdf (ring-io/piped-input-stream
                           (fn [out]
                             (pdf/convert rendered-docx out)))]
        (doc-store/store (bank-notification-s3-key case-id bank-id) rendered-pdf)))
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id bank-id :started)))
    {:status 204}))

(defn cancel-notification [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt/await-tx xtdb-node
      (xt/submit-tx xtdb-node
        (change-bank-notification-status-txns case-id bank-id :cancelled)))
    {:status 204}))

(defn get-notification-pdf [{:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        input-stream (doc-store/fetch
                       (bank-notification-s3-key case-id bank-id))]
    {:status 200
     :headers {"Content-Type" "application/pdf"}
     :body input-stream}))

(defn routes []
  [["/case/:case-id/bank/:bank-id"
    ["/start-notification" {:post {:handler start-notification}}]
    ["/cancel-notification" {:post {:handler cancel-notification}}]
    ["/notification-pdf" {:get {:handler get-notification-pdf}}]]])

(comment
  (def case-data
    (fetch-one
      (xt/q (xt/db darbylaw.xtdb-node/xtdb-node) get-case--query
        #uuid"041c820a-2e09-4b73-8452-0d0c3feb281b")))

  (with-open [in (render-docx (case-template-data :britannia-bereavement-team case-data))
              out (io/output-stream "test.docx")]
    (io/copy in out))

  (with-open [in (render-docx (case-template-data :britannia-bereavement-team case-data))
              out (io/output-stream "test.pdf")]
    (pdf/convert in out))

  (let [resp (get-notification-pdf
               {:path-params
                {:case-id "041c820a-2e09-4b73-8452-0d0c3feb281b"
                 :bank-id :britannia-bereavement-team}})]
    (with-open [w (io/output-stream "test.pdf")]
      (io/copy (:body resp) w)))
  ,)
