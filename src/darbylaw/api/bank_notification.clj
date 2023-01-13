(ns darbylaw.api.bank-notification
  (:require [xtdb.api :as xt]
            [clojure.java.io :as io]
            [mount.core :as mount]
            [darbylaw.api.bank-notification-template :as template]
            [darbylaw.api.death-cert-verif-template :as death-cert-template]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.pdf :as pdf]
            [pdfboxing.merge :as pdf-merge]
            [pdfboxing.info :as pdf-info]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.files :as files-util :refer [with-delete]]
            [darbylaw.api.bank-notification.letter-store :as letter-store]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.data :as data-util]))

(defn build-asset-id [bank-type case-id bank-id]
  {:type (case bank-type
           :bank :probate.bank-accounts
           :buildsoc :probate.buildsoc-accounts)
   :case-id case-id
   (case bank-type
     :bank :bank-id
     :buildsoc :buildsoc-id) bank-id})

(mount/defstate blank-page
  :start (io/resource "darbylaw/templates/blank-page.pdf"))

(defn convert-to-pdf [xtdb-node case-id bank-id letter-id docx]
  (with-delete [letter-pdf (files-util/create-temp-file (str letter-id "-interim") ".pdf")
                death-cert-docx (files-util/create-temp-file (str letter-id "-death-cert") ".docx")
                death-cert-pdf (files-util/create-temp-file (str letter-id "-death-cert") ".pdf")]
    (let [;; Don't clean up final-pdf as we need to return it
          final-pdf (files-util/create-temp-file letter-id ".pdf")
          template-data (death-cert-template/get-letter-template-data
                          xtdb-node case-id)]
      (pdf/convert-file docx letter-pdf)
      (death-cert-template/render-docx template-data death-cert-docx)
      (pdf/convert-file death-cert-docx death-cert-pdf)
      (pdf-merge/merge-pdfs
        :input (->> [(.getAbsolutePath letter-pdf)
                     ;; We want death-cert-pdf to be on an odd numbered
                     ;; page so that when the final-pdf is printed
                     ;; double sided it's on it's own page.
                     ;; To do this we insert a blank page when
                     ;; letter-pdf has an odd number of pages.
                     (when (odd? (pdf-info/page-number letter-pdf))
                       (.toURI blank-page))
                     (.getAbsolutePath death-cert-pdf)]
                    (remove nil?))
        :output (.getAbsolutePath final-pdf))
      final-pdf)))

(defn convert-to-pdf-and-store [xtdb-node case-id bank-id letter-id docx]
  (with-delete [final-pdf (convert-to-pdf xtdb-node case-id bank-id letter-id docx)]
    (doc-store/store (letter-store/s3-key case-id bank-id letter-id ".docx") docx)
    (doc-store/store (letter-store/s3-key case-id bank-id letter-id ".pdf") final-pdf)))

(defn generate-notification-letter [{:keys [xtdb-node user path-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        ; First check for existance. Prevent generating docs if not needed.
        asset-id (build-asset-id bank-type case-id bank-id)
        assert-letter-not-exists-tx (tx-fns/assert-nil asset-id [:notification-letter])
        tx1 (xt-util/exec-tx xtdb-node assert-letter-not-exists-tx)]
    (if-not (xt/tx-committed? xtdb-node tx1)
      {:status http/status-409-conflict
       :body {:error :already-exists}}
      (let [letter-template-data (template/get-letter-template-data xtdb-node bank-type case-id bank-id)
            letter-id (str (:reference letter-template-data)
                        "." (name bank-id)
                        ".bank-notification."
                        (random-uuid))]
        (with-delete [docx (files-util/create-temp-file letter-id ".docx")]
          (template/render-docx bank-type letter-template-data docx)
          (convert-to-pdf-and-store xtdb-node case-id bank-id letter-id docx))
        (let [tx2 (xt-util/exec-tx xtdb-node
                    (concat
                      ; Second check inside tx.
                      assert-letter-not-exists-tx
                      [[::xt/put {:type :probate.bank-notification-letter
                                  :xt/id letter-id
                                  :case-id case-id
                                  :bank-type bank-type
                                  :bank-id bank-id
                                  :author :generated
                                  :by (:username user)}]]
                      (tx-fns/set-value asset-id [:notification-letter] letter-id)
                      (case-history/put-event
                        {:event :bank-notification.letter-generated
                         :case-id case-id
                         :user user
                         :bank-id bank-id
                         :letter-id letter-id})))]
          (if (xt/tx-committed? xtdb-node tx2)
            {:status 204}
            {:status http/status-409-conflict
             :body {:error :already-exists}}))))))

(defn regenerate-notification-letter [{:keys [xtdb-node user path-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        asset-id (build-asset-id bank-type case-id bank-id)
        letter-id (:letter-id path-params)
        asset (xt/pull (xt/db xtdb-node) '[{:notification-letter [*]}] asset-id)
        letter (:notification-letter asset)]
    (cond
      (not= (:xt/id letter) letter-id)
      {:status http/status-404-not-found}

      (some? (:send-action letter))
      {:status http/status-409-conflict}

      :else
      ; There is a race-condition here, that could happen if 2 users are regenerating concurrently.
      ; It could be solved if we generated a new bank-notification-letter altogether,
      ; but then we'd need to deal with deleting S3 files.
      (let [letter-template-data (template/get-letter-template-data xtdb-node bank-type case-id bank-id)]
        (with-delete [docx (files-util/create-temp-file letter-id ".docx")]
          (template/render-docx bank-type letter-template-data docx)
          (convert-to-pdf-and-store xtdb-node case-id bank-id letter-id docx))
        (xt-util/exec-tx xtdb-node
          (concat
            (tx-fns/set-values letter-id
              {:author :generated
               :by (:username user)
               :review-by nil
               :review-timestamp nil})
            (case-history/put-event
              {:event :bank-notification.letter-generated
               :case-id case-id
               :user user
               :bank-id bank-id
               :letter-id letter-id})))
        {:status http/status-204-no-content}))))

(def docx-mime-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

(defn fetch-letter-id [xtdb-node asset-id]
  (-> (xt/pull (xt/db xtdb-node)
        [:notification-letter] asset-id)
    :notification-letter))

(defn post-notification [{:keys [xtdb-node user path-params multipart-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")
        asset-id (build-asset-id bank-type case-id bank-id)
        letter-id (fetch-letter-id xtdb-node asset-id)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [username (:username user)]
        (with-delete [tempfile tempfile]
          (assert (= content-type docx-mime-type))
          (convert-to-pdf-and-store xtdb-node case-id bank-id letter-id tempfile))
        (xt-util/exec-tx xtdb-node
          (concat
            (tx-fns/set-value letter-id [:author] username)
            (tx-fns/set-value letter-id [:by] username)
            (case-history/put-event
              {:event :bank-notification.letter-replaced
               :case-id case-id
               :user user
               :bank-id bank-id
               :letter-id letter-id})))
        {:status http/status-204-no-content}))))

(defn get-notification [doc-type {:keys [xtdb-node path-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        asset-id (build-asset-id bank-type case-id bank-id)
        letter-id (fetch-letter-id xtdb-node asset-id)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [input-stream (doc-store/fetch
                           (letter-store/s3-key case-id bank-id letter-id
                             (case doc-type
                               :docx ".docx"
                               :pdf ".pdf")))]
        {:status http/status-200-ok
         :headers {"Content-Type" (case doc-type
                                    :docx docx-mime-type
                                    :pdf "application/pdf")}
         :body input-stream}))))

(defn notification-letter-review [{:keys [xtdb-node path-params user body-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        asset-id (build-asset-id bank-type case-id bank-id)
        letter-id (:letter-id path-params)
        send-action (:send-action body-params)]
    (assert (#{:send :fake-send :do-not-send} send-action))
    (let [tx (xt-util/exec-tx xtdb-node
               (concat
                 (tx-fns/assert-equals asset-id [:notification-letter] letter-id)
                 (tx-fns/set-value letter-id [:review-by] (:username user))
                 (tx-fns/set-value letter-id [:review-timestamp] (xt-util/now))
                 (tx-fns/set-value letter-id [:send-action] send-action)
                 (case-history/put-event
                   {:event :bank-notification.letter-reviewed
                    :case-id case-id
                    :user user
                    :bank-id bank-id
                    :letter-id letter-id})))]
      (if (xt/tx-committed? xtdb-node tx)
        {:status http/status-204-no-content}
        {:status http/status-404-not-found}))))

(defn get-valuation [{:keys [xtdb-node path-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        asset-id (build-asset-id bank-type case-id bank-id)
        {letter-id :valuation-letter} (xt/pull (xt/db xtdb-node)
                                        [:valuation-letter] asset-id)
        input-stream (doc-store/fetch
                       (letter-store/s3-key case-id bank-id letter-id ".pdf"))]
    {:status 200
     :headers {"Content-Type" "application/pdf"}
     :body input-stream}))

(defn post-valuation [{:keys [xtdb-node user path-params multipart-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")
        _ (assert (= content-type "application/pdf"))
        filename (get multipart-params "filename")
        letter-id (str (random-uuid)
                       "."
                       (data-util/strip-end filename ".pdf"))]
    (with-delete [tempfile tempfile]
      (doc-store/store (letter-store/s3-key case-id bank-id letter-id ".pdf") tempfile))
    (do
      (xt-util/exec-tx xtdb-node
        (concat
          [[::xt/put {:type :probate.received-bank-letter
                      :xt/id letter-id
                      :case-id case-id
                      :bank-id bank-id
                      :original-filename filename
                      :contains-valuation true
                      :uploaded-by (:username user)
                      :uploaded-at (xt-util/now)}]]
          ; This should be obsolete when we support multiple valuation letters:
          (tx-fns/set-value (build-asset-id bank-type case-id bank-id)
            [:valuation-letter] letter-id)
          (case-history/put-event
            {:event :bank-notification.valuation-letter-updated
             :user user
             :case-id case-id
             :bank-id bank-id
             :letter-id letter-id}))))
    {:status 204}))

(defn mark-values-confirmed [{:keys [xtdb-node user path-params bank-type]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value (build-asset-id bank-type case-id bank-id)
          [:values-confirmed]
          {:by (:username user)
           :timestamp (xt-util/now)})
        (case-history/put-event
          {:event :bank-notification.values-confirmed
           :case-id case-id
           :user user
           :bank-id bank-id})))
    {:status 204}))

(defn wrap-bank-type [handler bank-type]
  (fn [req]
    (handler (-> req
               (assoc :bank-type bank-type)))))

(def common-routes
  [["/generate-notification-letter"
    {:post {:handler generate-notification-letter}}]
   ["/notification-letter/:letter-id/regenerate"
    {:post {:handler regenerate-notification-letter}}]
   ["/notification-pdf"
    {:get {:handler (partial get-notification :pdf)}}]
   ["/notification-docx"
    {:get {:handler (partial get-notification :docx)}
     :post {:handler post-notification}}]
   ["/notification-letter/:letter-id/review"
    {:post {:handler notification-letter-review}}]
   ["/valuation-pdf"
    {:get {:handler get-valuation}
     :post {:handler post-valuation}}]
   ["/mark-values-confirmed"
    {:post {:handler mark-values-confirmed}}]])

(defn routes []
  [["/case/:case-id/bank/:bank-id"
    {:middleware [[wrap-bank-type :bank]]}
    common-routes]
   ["/case/:case-id/buildsoc/:bank-id"
    {:middleware [[wrap-bank-type :buildsoc]]}
    common-routes]])

(comment
  (def all-case-data
    (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node)
      #uuid"cd245cf8-ebc2-4703-9833-c0aaa3376c0b")))
