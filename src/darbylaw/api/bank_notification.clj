(ns darbylaw.api.bank-notification
  (:require [xtdb.api :as xt]
            [darbylaw.api.bank-notification-template :as template]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.pdf :as pdf]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.files :as files-util]
            [darbylaw.api.bank-notification.post-task :as post-task]
            [darbylaw.api.bank-notification.letter-store :as letter-store]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]))

(defn build-asset-id [case-id bank-id]
  {:type :probate.bank-accounts
   :case-id case-id
   :bank-id bank-id})

(defn convert-to-pdf-and-store [case-id bank-id docx]
  (let [pdf (files-util/create-temp-file ".pdf")]
    (try
      (pdf/convert-file docx pdf)
      (doc-store/store (letter-store/s3-key case-id bank-id ".docx") docx)
      (doc-store/store (letter-store/s3-key case-id bank-id ".pdf") pdf)
      (finally
        (.delete pdf)))))

(defn generate-notification-letter [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        letter-template-data (template/get-letter-template-data xtdb-node case-id bank-id)
        docx (files-util/create-temp-file ".docx")]
    (try
      (template/render-docx letter-template-data docx)
      (convert-to-pdf-and-store case-id bank-id docx)
      (finally
        (.delete docx)))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value (build-asset-id case-id bank-id)
          [:notification-letter :author]
          :generated)
        (case-history/put-event
          {:event :bank-notification.letter-generated
           :case-id case-id
           :user user
           :bank-id bank-id})))
    {:status 204}))

(def docx-mime-type
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

(defn post-notification [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")]
    (try
      (assert (= content-type docx-mime-type))
      (convert-to-pdf-and-store case-id bank-id tempfile)
      (finally
        (.delete tempfile)))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value (build-asset-id case-id bank-id)
          [:notification-letter :author]
          user)
        (case-history/put-event
          {:event :bank-notification.letter-updated
           :case-id case-id
           :user user
           :bank-id bank-id})))
    {:status 204}))

(defn get-notification [doc-type {:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        input-stream (doc-store/fetch
                       (letter-store/s3-key case-id bank-id (case doc-type
                                                              :docx ".docx"
                                                              :pdf ".pdf")))]
    {:status 200
     :headers {"Content-Type"
               (case doc-type
                 :docx docx-mime-type
                 :pdf "application/pdf")}
     :body input-stream}))

(defn approve-notification-letter [{:keys [xtdb-node path-params user]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        created? (post-task/create-post-task! xtdb-node case-id bank-id)]
    (if (not created?)
      {:status http/status-409-conflict}
      (do
        (xt-util/exec-tx xtdb-node
          (concat
            (tx-fns/set-value (build-asset-id case-id bank-id)
              [:notification-letter :approved]
              {:by user
               :timestamp (xt-util/now)})
            (case-history/put-event
              {:event :bank-notification.letter-approved
               :case-id case-id
               :user user
               :bank-id bank-id})))
        {:status http/status-204-no-content}))))

(defn get-valuation [{:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        input-stream (doc-store/fetch
                       (letter-store/s3-key case-id bank-id "-valuation.pdf"))]
    {:status 200
     :headers {"Content-Type" "application/pdf"}
     :body input-stream}))

(defn post-valuation [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")]
    (try
      (assert (= content-type "application/pdf"))
      (doc-store/store (letter-store/s3-key case-id bank-id "-valuation.pdf") tempfile)
      (finally
        (.delete tempfile)))
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value (build-asset-id case-id bank-id)
          [:valuation-letter-uploaded]
          {:by user
           :timestamp (xt-util/now)})
        (case-history/put-event
          {:event :bank-notification.valuation-letter-updated
           :case-id case-id
           :user user
           :bank-id bank-id})))
    {:status 204}))

(defn mark-values-confirmed [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (keyword (:bank-id path-params))]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-value (build-asset-id case-id bank-id)
          [:values-confirmed]
          {:by user
           :timestamp (xt-util/now)})
        (case-history/put-event
          {:event :bank-notification.values-confirmed
           :case-id case-id
           :user user
           :bank-id bank-id})))
    {:status 204}))

(defn get-post-tasks [{:keys [xtdb-node]}]
  {:status http/status-200-ok
   :body (->> (xt/q (xt/db xtdb-node)
                '{:find [(pull task [:case-id
                                     :bank-id
                                     :post-state
                                     :created-at])]
                  :where [[task :type task-type]]
                  :in [task-type]}
                post-task/task-type)
           (map (fn [[post-task]] (-> post-task))))})

(defn routes []
  [["/case/:case-id/bank/:bank-id"
    ["/generate-notification-letter" {:post {:handler generate-notification-letter}}]
    ["/notification-docx" {:get {:handler (partial get-notification :docx)}
                           :post {:handler post-notification}}]
    ["/notification-pdf" {:get {:handler (partial get-notification :pdf)}}]
    ["/approve-notification-letter" {:post {:handler approve-notification-letter}}]
    ["/valuation-pdf" {:get {:handler get-valuation}
                       :post {:handler post-valuation}}]
    ["/mark-values-confirmed" {:post {:handler mark-values-confirmed}}]]
   ["/post-tasks" {:get {:handler get-post-tasks}}]])

(comment
  (def all-case-data
    (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node)
      #uuid"cd245cf8-ebc2-4703-9833-c0aaa3376c0b")))
