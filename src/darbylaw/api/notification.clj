(ns darbylaw.api.notification
  (:require
    [clojure.java.io :as io]
    [darbylaw.api.bank-list :as banks]
    [darbylaw.api.bank-notification-template :as template]
    [darbylaw.api.bank-notification.letter-store :as letter-store]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.pdf :as pdf]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.files :as files-util]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.doc-store :as doc-store]
    [stencil.api :as stencil]
    [xtdb.api :as xt]))

(defn build-asset-id [case-id asset-id type]
  {:type (keyword (str "probate." type "-accounts"))
   :case-id case-id
   (keyword (str type "-id")) asset-id})

(defn bank-letter-template-query [case-id bank-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull bank-accounts [:accounts])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [bank-accounts :type :probate.bank-accounts]
             [bank-accounts :bank-id bank-id]
             [bank-accounts :case-id case-id]]
    :in '[case-id
          bank-id]}
   case-id
   bank-id])

(defn buildsoc-letter-template-query [case-id buildsoc-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull buildsoc-accounts [:accounts])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [buildsoc-accounts :type :probate.buildsoc-accounts]
             [buildsoc-accounts :buildsoc-id buildsoc-id]
             [buildsoc-accounts :case-id case-id]]
    :in '[case-id
          buildsoc-id]}
   case-id
   buildsoc-id])

(defn get-letter-template-data [xtdb-node case-id institution-id type]
  (let [[case-data accounts-data] (if (= type "bank") (xt-util/fetch-one
                                                        (apply xt/q (xt/db xtdb-node)
                                                          (bank-letter-template-query case-id institution-id)))
                                                      (xt-util/fetch-one ;gets 3 results
                                                        (apply xt/q (xt/db xtdb-node)
                                                          (buildsoc-letter-template-query case-id institution-id))))]

    ;creates eg {%rollNumber%} to insert into template
    (data-util/keys-to-camel-case
      (if (= type "bank")
        (-> case-data
          (assoc :bank (-> accounts-data
                         (assoc :name (banks/bank-label institution-id)))))
        (-> case-data
          (assoc :buildsoc (-> accounts-data
                             (assoc :name institution-id))))))))

(def bank-notification-template
  (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx")))

(def buildsoc-notification-template
  (stencil/prepare (io/resource "darbylaw/templates/buildsoc-notification.docx")))

(defn render-docx [template-data file]
  (stencil/render! bank-notification-template template-data
    :output file
    :overwrite? true))

(defn convert-to-pdf-and-store [case-id institution-id letter-id docx]
  (let [pdf (files-util/create-temp-file letter-id ".pdf")]
    (try
      (pdf/convert-file docx pdf)
      (doc-store/store (letter-store/s3-key case-id institution-id letter-id ".docx") docx)
      (doc-store/store (letter-store/s3-key case-id institution-id letter-id ".pdf") pdf)
      (finally
        (.delete pdf)))))

(defn generate-notification-letter [type {:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        id-keyword (keyword (str type "-id"))
        institution-id (keyword (id-keyword path-params))
        ; First check for existance. Prevent generating docs if not needed.
        asset-id (build-asset-id case-id institution-id type)
        assert-letter-not-exists-tx (tx-fns/assert-nil asset-id [:notification-letter])
        tx1 (xt-util/exec-tx xtdb-node assert-letter-not-exists-tx)]
    #_(if-not (xt/tx-committed? xtdb-node tx1)
        {:status http/status-409-conflict
         :body {:error :already-exists}})
    (let [letter-template-data (get-letter-template-data xtdb-node case-id institution-id type)
          letter-id (str (:reference letter-template-data)
                      "." (name institution-id)
                      "." type "-notification."
                      (random-uuid))
          docx (files-util/create-temp-file letter-id ".docx")]
      (try
        (render-docx letter-template-data docx)
        (convert-to-pdf-and-store case-id institution-id letter-id docx)
        (finally
          (.delete docx)))
      (let [type-string (keyword (str "probate." type "-notification-letter"))
            id-keyword (keyword (str type "-id"))
            tx2 (xt-util/exec-tx xtdb-node
                  (concat
                    ; Second check inside tx.
                    assert-letter-not-exists-tx
                    [[::xt/put {:type type-string
                                :xt/id letter-id
                                :case-id case-id
                                id-keyword institution-id
                                :author :generated
                                :by (:username user)}]]
                    (tx-fns/set-value asset-id [:notification-letter] letter-id)
                    (case-history/put-event
                      {:event (keyword (str type "-notification.letter-generated"))
                       :case-id case-id
                       :user user
                       id-keyword institution-id
                       :letter-id letter-id})))]
        (if (xt/tx-committed? xtdb-node tx2)
          {:status 204}
          {:status http/status-409-conflict
           :body {:error :already-exists}})))))

(defn fetch-letter-id [xtdb-node asset-id]
  (-> (xt/pull (xt/db xtdb-node)
        [:notification-letter] asset-id)
    :notification-letter))

(def docx-mime-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

(defn get-notification [type doc-type {:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        id-keyword (keyword (str type "-id"))
        institution-id (keyword (id-keyword path-params))
        asset-id (build-asset-id case-id institution-id type)
        letter-id (fetch-letter-id xtdb-node asset-id)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [input-stream (doc-store/fetch
                           (letter-store/s3-key case-id institution-id letter-id
                             (case doc-type
                               :docx ".docx"
                               :pdf ".pdf")))]
        {:status http/status-200-ok
         :headers {"Content-Type" (case doc-type
                                    :docx docx-mime-type
                                    :pdf "application/pdf")}
         :body input-stream}))))

(defn post-notification [type {:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        id-keyword (keyword (str type "-id"))
        institution-id (keyword (id-keyword path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")
        asset-id (build-asset-id case-id institution-id type)
        letter-id (fetch-letter-id xtdb-node asset-id)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [username (:username user)]
        (try
          (assert (= content-type docx-mime-type))
          (convert-to-pdf-and-store case-id institution-id letter-id tempfile)
          (finally
            (.delete tempfile)))
        (xt-util/exec-tx xtdb-node
          (concat
            ;TODO check are author and by the same thing?
            (tx-fns/set-value letter-id [:author] username)
            (tx-fns/set-value letter-id [:by] username)
            (case-history/put-event
              {:event (keyword (str type "-notification.letter-replaced"))
               :case-id case-id
               :user user
               id-keyword institution-id
               :letter-id letter-id})))
        {:status http/status-204-no-content}))))

(defn approve-notification-letter [type {:keys [xtdb-node path-params user]}]
  (let [case-id (parse-uuid (:case-id path-params))
        id-keyword (keyword (str type "-id"))
        institution-id (keyword (id-keyword path-params))
        asset-id (build-asset-id case-id institution-id type)
        letter-id (:letter-id path-params)]
    (let [tx (xt-util/exec-tx xtdb-node
               (concat
                 (tx-fns/assert-equals asset-id [:notification-letter] letter-id)
                 (tx-fns/set-value letter-id [:approved] {:by (:username user)
                                                          :timestamp (xt-util/now)})
                 ;TODO do we want to remove this notification-status key and instead derive it from :notification-letter?
                 (tx-fns/set-value asset-id [:notification-status] :approved)
                 (case-history/put-event
                   {:event (keyword (str type "-notification.letter-approved"))
                    :case-id case-id
                    :user user
                    id-keyword institution-id
                    :letter-id letter-id})))]
      (if (xt/tx-committed? xtdb-node tx)
        {:status http/status-204-no-content}
        {:status http/status-404-not-found}))))

(defn routes []
  [["/case/:case-id/buildsoc/:buildsoc-id"
    ["/generate-notification-letter" {:post {:handler (partial generate-notification-letter "buildsoc")}}]
    ["/notification-pdf" {:get {:handler (partial get-notification "buildsoc" :pdf)}}]
    ["/notification-docx" {:get {:handler (partial get-notification "buildsoc" :docx)}
                           :post {:handler (partial post-notification "buildsoc")}}]
    ["/approve-notification-letter/:letter-id" {:post {:handler (partial approve-notification-letter "buildsoc")}}]]])
;["/case/:case-id/bank/:bank-id"
;["/generate-notification-letter" {:post {:handler (partial generate-notification-letter "bank")}}]]])