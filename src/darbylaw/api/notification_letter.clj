(ns darbylaw.api.notification-letter
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [darbylaw.api.death-cert-verif-template :as death-cert-template]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [mount.core :as mount]
    [pdfboxing.info :as pdf-info]
    [pdfboxing.merge :as pdf-merge]
    [xtdb.api :as xt]
    [darbylaw.doc-store :as doc-store]
    [darbylaw.api.pdf :as pdf]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.files :as files-util :refer [with-delete]]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.bill.notification-template :as template]
    [darbylaw.api.letter :as letter]
    [darbylaw.api.pensions :as pensions]))

(mount/defstate blank-page
  :start (io/resource "darbylaw/templates/blank-page.pdf"))
(defn convert-to-pdf-and-store [institution xtdb-node case-id letter-id docx]
  (if (= institution :state)
    (with-delete [pdf (files-util/create-temp-file letter-id ".pdf")
                  death-cert-docx (files-util/create-temp-file (str letter-id "-death-cert") ".docx")
                  death-cert-pdf (files-util/create-temp-file (str letter-id "-death-cert") ".pdf")]
      (let [;; Don't clean up final-pdf as we need to return it
            final-pdf (files-util/create-temp-file letter-id ".pdf")
            template-data (death-cert-template/get-letter-template-data
                            xtdb-node case-id)]
        (pdf/convert-file docx pdf)
        (death-cert-template/render-docx template-data death-cert-docx)
        (pdf/convert-file death-cert-docx death-cert-pdf)
        (pdf-merge/merge-pdfs
          :input (->> [(.getAbsolutePath pdf)
                       ;; We want death-cert-pdf to be on an odd numbered
                       ;; page so that when the final-pdf is printed
                       ;; double sided it's on it's own page.
                       ;; To do this we insert a blank page when
                       ;; letter-pdf has an odd number of pages.
                       (when (odd? (pdf-info/page-number pdf))
                         (io/as-file blank-page))
                       (.getAbsolutePath death-cert-pdf)]
                     (remove nil?))
          :output (.getAbsolutePath final-pdf))
        (doc-store/store-case-file case-id (str letter-id ".pdf") final-pdf)))

    (with-delete [pdf (files-util/create-temp-file letter-id ".pdf")]
      (pdf/convert-file docx pdf)
      (doc-store/store-case-file case-id (str letter-id ".docx") docx)
      (doc-store/store-case-file case-id (str letter-id ".pdf") pdf))))

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
                      :council-tax (:council body-params)
                      :pension (:provider body-params))
        asset-id (:asset-id body-params)
        template-data (case notification-type
                        :utility (template/get-letter-data xtdb-node :utility case-id institution property-id)
                        :council-tax (template/get-letter-data xtdb-node :council-tax case-id institution property-id)
                        :pension (pensions/get-letter-data xtdb-node case-id (parse-uuid asset-id)))
        _ (assert (:reference template-data))
        letter-id (str/join "."
                    [(:reference template-data)
                     "notification"
                     (name notification-type)
                     (case notification-type
                       :utility (name (:utility-company body-params))
                       :council-tax (name (:council body-params))
                       :pension (name (:provider body-params)))
                     (random-uuid)])]

    (with-delete [docx (files-util/create-temp-file letter-id ".docx")]
      (case notification-type
        :utility (template/render-docx :utility template-data docx)
        :council-tax (template/render-docx :council-tax template-data docx)
        :pension (pensions/render-docx (:pension-type body-params) template-data docx))
      (convert-to-pdf-and-store institution xtdb-node case-id letter-id docx))
    (let [specific-props (case notification-type
                           :utility (select-mandatory body-params [:utility-company
                                                                   :property])
                           :council-tax (select-mandatory body-params [:council
                                                                       :property])
                           :pension (select-mandatory body-params [:provider :pension-type]))]
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
          (case-history/put-event2
            (merge {:case-id case-id
                    :user user
                    :subject :probate.case.notification-letter
                    :op :generated
                    :letter letter-id
                    :institution-type (letter/get-institution-type notification-type)
                    :institution (letter/get-institution-id notification-type specific-props)}
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
                 (let [letter-data (xt/entity (xt/db xtdb-node) letter-id)
                       notification-type (:notification-type letter-data)]
                   (case-history/put-event2
                     {:case-id case-id
                      :user user
                      :subject :probate.case.notification-letter
                      :op send-action
                      :institution-type (letter/get-institution-type notification-type)
                      :institution (letter/get-institution-id notification-type letter-data)
                      :letter letter-id}))))]
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
          (convert-to-pdf-and-store nil xtdb-node case-id letter-id tempfile))
        (xt-util/exec-tx xtdb-node
          (concat
            (tx-fns/set-values letter-id
              {:author username
               :by username
               :modified-at (xt-util/now)})
            (let [letter-data (xt/entity (xt/db xtdb-node) letter-id)
                  notification-type (:notification-type letter-data)]
              (case-history/put-event2
                {:case-id case-id
                 :user user
                 :subject :probate-case.notification-letter
                 :op :replaced
                 :institution-type (letter/get-institution-type notification-type)
                 :institution (letter/get-institution-id notification-type letter-data)
                 :letter letter-id}))))
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
