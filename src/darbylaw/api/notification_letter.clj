(ns darbylaw.api.notification-letter
  (:require
    [clojure.string :as str]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [xtdb.api :as xt]
    [darbylaw.doc-store :as doc-store]
    [darbylaw.api.pdf :as pdf]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.files :as files-util :refer [with-delete]]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.bill.notification-template :as template]))

(defn convert-to-pdf-and-store [case-id letter-id docx]
  (with-delete [pdf (files-util/create-temp-file letter-id ".pdf")]
    (pdf/convert-file docx pdf)
    (doc-store/store-case-file case-id (str letter-id ".docx") docx)
    (doc-store/store-case-file case-id (str letter-id ".pdf") pdf)))

(defn select-mandatory [m ks]
  (doseq [k ks]
    (assert (get m k)
      (str "Missing mandatory key " k)))
  (select-keys m ks))

(defn generate-notification-letter [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        notification-type (:notification-type body-params)
        property-id (:property body-params)
        institution (case notification-type
                      :utility (:utility-company body-params)
                      :council-tax (:council body-params))
        asset-id (:asset-id body-params)
        template-data (case notification-type
                        :utility (template/get-letter-data xtdb-node :utility case-id institution property-id)
                        :council-tax (template/get-letter-data xtdb-node :council-tax case-id institution property-id))
        _ (assert (:reference template-data))
        letter-id (str/join "."
                    [(:reference template-data)
                     "notification"
                     (name notification-type)
                     (case notification-type
                       :utility (name (:utility-company body-params))
                       :council-tax (name (:council body-params)))
                     (random-uuid)])]
    (with-delete [docx (files-util/create-temp-file letter-id ".docx")]
      (case notification-type
        :utility (template/render-docx :utility template-data docx)
        :council-tax (template/render-docx :council-tax template-data docx))
      (convert-to-pdf-and-store case-id letter-id docx))
    (let [specific-props (case notification-type
                           :utility (select-mandatory body-params [:utility-company
                                                                   :property])
                           :council-tax (select-mandatory body-params [:council
                                                                       :property]))]
      (xt-util/exec-tx-or-throw xtdb-node
        (concat
          [[::xt/put (merge {:type :probate.notification-letter
                             :xt/id letter-id
                             :probate.notification-letter/case case-id
                             :author :generated
                             :by (:username user)
                             :modified-at (xt-util/now)
                             :notification-type notification-type}
                            specific-props)]]
          (case-history/put-event (merge {:event :notification.letter-generated
                                          :case-id case-id
                                          :user user
                                          :letter-id letter-id
                                          :notification-type notification-type}
                                         specific-props)))))
    {:status http/status-204-no-content}))

(comment
  (require '[darbylaw.xtdb-node])
  (def all-letters
    (->> (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
           {:find '[(pull letter [*])]
            :where '[[letter :type :probate.notification-letter]]})
      (map first)))
  (doseq [letter all-letters]
    (xt/submit-tx darbylaw.xtdb-node/xtdb-node
      [[::xt/delete (:xt/id letter)]])))

(defn get-notification-letter [doc-type {:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        letter-id (:letter-id path-params)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [input-stream (doc-store/fetch-case-file case-id
                           (str letter-id (case doc-type
                                            :docx ".docx"
                                            :pdf ".pdf")))]
        {:status http/status-200-ok
         :headers {"Content-Type" (case doc-type
                                    :docx http/docx-mime-type
                                    :pdf http/pdf-mime-type)}
         :body input-stream}))))

(defn send-notification-letter [{:keys [xtdb-node path-params user body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        letter-id (:letter-id path-params)
        send-action (:send-action body-params)]
    (assert (#{:send :fake-send} send-action))
    (let [tx (xt-util/exec-tx xtdb-node
               (concat
                 (tx-fns/assert-equals letter-id [:probate.notification-letter/case] case-id)
                 (tx-fns/assert-nil letter-id [:mail/send-action])
                 (tx-fns/set-value letter-id [:mail/send-action] send-action)
                 (tx-fns/set-value letter-id [:sent-by] (:username user))
                 (tx-fns/set-value letter-id [:sent-at] (xt-util/now))
                 (case-history/put-event
                   {:event :notification.letter-sent
                    :case-id case-id
                    :user user
                    :letter-id letter-id})))]
      (if (xt/tx-committed? xtdb-node tx)
        {:status http/status-204-no-content}
        {:status http/status-404-not-found}))))

(defn post-notification-letter [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        letter-id (:letter-id path-params)
        {:keys [tempfile content-type]} (get multipart-params "file")]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [username (:username user)]
        (assert (= content-type http/docx-mime-type))
        (with-delete [tempfile tempfile]
          (convert-to-pdf-and-store case-id letter-id tempfile))
        (xt-util/exec-tx xtdb-node
          (concat
            (tx-fns/set-values letter-id
              {:author username
               :by username
               :modified-at (xt-util/now)})
            (case-history/put-event
              {:event :notification.letter-replaced
               :case-id case-id
               :user user
               :letter-id letter-id})))
        {:status http/status-204-no-content}))))

(defn routes []
  [["/case/:case-id"
    ["/generate-notification-letter"
     {:post {:handler generate-notification-letter}}]
    ["/notification-letter/:letter-id/pdf"
     {:get {:handler (partial get-notification-letter :pdf)}}]
    ["/notification-letter/:letter-id/docx"
     {:get {:handler (partial get-notification-letter :docx)}
      :post {:handler post-notification-letter}}]
    ["/notification-letter/:letter-id/send"
     {:post {:handler send-notification-letter}}]]])
