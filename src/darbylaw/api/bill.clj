(ns darbylaw.api.bill
  (:require [darbylaw.api.bill.data :as bill-data]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.files :refer [with-delete]]
            [xtdb.api :as xt]))

(def bill-creation-schema
  (bill-data/make-bill-schema :create))

(def creation-props
  (bill-data/extract-bill-props bill-creation-schema))

(def council-tax-creation-schema
  (bill-data/make-council-tax-schema :create))

(defn council-tax-props [op]
  (bill-data/extract-council-tax-props
    (bill-data/make-council-tax-schema op)))

(comment
  (require '[malli.core :as malli])
  (malli.core/explain bill-creation-schema {:bill-type #{:other}
                                            ;:issuer :utility-1
                                            :amount "10"
                                            :custom-issuer-name "hola"
                                            :custom-issuer-address "addr"
                                            :address "hey"}))

(defn handle-property [{:keys [user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        property (:property body-params)]
    (cond
      (string? property)
      (let [new-property-id (random-uuid)]
        [new-property-id (concat
                           [[::xt/put {:xt/id new-property-id
                                       :type :probate.property
                                       :probate.property/case case-id
                                       :address property}]]
                           (case-history/put-event
                             {:event :properties-updated
                              :case-id case-id
                              :user user
                              :op :add
                              :event/property new-property-id}))])
      (uuid? property)
      [property (tx-fns/assert-exists property)]

      :else
      (assert false (str "Unexpected type:" (type property))))))

(defn add-bill [{:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        bill-id (random-uuid)
        [property-id property-tx] (handle-property args)
        bill-data (-> body-params
                    (select-keys creation-props)
                    (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        [[::xt/put (merge bill-data
                     {:xt/id bill-id
                      :type :probate.bill
                      :probate.bill/case case-id})]]
        (case-history/put-event {:event :utility-added
                                 :case-id case-id
                                 :user user
                                 :op :add
                                 :event/bill bill-id}))))
  {:status http/status-204-no-content})

(defn delete-bill [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bill-type (if (contains? path-params :bill-id) :utility :council-tax)
        bill-id (case bill-type
                  :utility (parse-uuid (:bill-id path-params))
                  :council-tax (parse-uuid (:council-tax-id path-params)))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/delete bill-id]]
        (case-history/put-event {:event (keyword (str bill-type "-deleted"))
                                 :case-id case-id
                                 :user user
                                 :op :delete
                                 :event/bill bill-id}))))
  {:status http/status-204-no-content})

(defn update-bill [{:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        [property-id property-tx] (handle-property args)
        bill-type (if (contains? path-params :bill-id) :utility :council-tax)
        bill-id (case bill-type
                  :utility (parse-uuid (:bill-id path-params))
                  :council-tax (parse-uuid (:council-tax-id path-params)))
        bill-data (-> body-params
                    (select-keys (case bill-type
                                   :utility creation-props
                                   :council-tax (council-tax-props :edit)))
                    (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        [[::xt/put (merge bill-data
                     {:xt/id bill-id
                      :type :probate.bill
                      (case bill-type
                        :utility :probate.bill/case
                        :council-tax :probate.council-tax/case) case-id})]]
        (case-history/put-event {:event (keyword (str bill-type "-updated"))
                                 :case-id case-id
                                 :user user
                                 :op :delete
                                 :event/bill bill-id}))))
  {:status http/status-204-no-content})

(defn add-council-tax [{:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        council-tax-id (random-uuid)
        [property-id property-tx] (handle-property args)
        council-tax-data (-> body-params
                           (select-keys (council-tax-props :create))
                           (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        [[::xt/put (merge council-tax-data
                     {:xt/id council-tax-id
                      :type :probate.council-tax
                      :probate.council-tax/case case-id})]]
        (tx-fns/append case-id [:council-tax] [council-tax-id])
        (case-history/put-event {:event :council-tax-added
                                 :case-id case-id
                                 :user user
                                 :op :add
                                 :event/bill council-tax-id}))) ;should this be event/council-tax?
    {:status 200
     :body {:property property-id
            :council (:council council-tax-data)}}))

(comment
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
          {:status http/status-204-no-content})))))

(def accepted-filetypes
  #{".pdf" ".png" ".jpeg" ".jpg" ".gif"})
(defn upload-document [asset-type {:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        asset-id (parse-uuid (:asset-id path-params))
        {:keys [tempfile]} (get multipart-params "file")
        reference (xt-util/get-reference xtdb-node case-id)
        orig-filename (get multipart-params "filename")
        extension (data-util/file-extension orig-filename)
        document-id (random-uuid)
        document-type (case asset-type
                        :utility :probate.utility-doc
                        :council-tax :probate.council-tax-doc)
        filename (str reference "." (name asset-type) "." document-id extension)]
    (assert (accepted-filetypes extension))
    (assert (not (clojure.string/blank? reference)))
    (with-delete [tempfile tempfile]
      (doc-store/store (str case-id "/" filename) tempfile))
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put {:type document-type
                    :xt/id filename
                    (keyword
                      (str document-type "/case")) case-id
                    (keyword
                      (str document-type "/asset")) asset-id
                    :uploaded-by (:username user)
                    :uploaded-at (xt-util/now)
                    :original-filename orig-filename}]]
        (tx-fns/set-value asset-id [:recent-bill] filename)
        (case-history/put-event
          {:event (keyword (str document-type ".uploaded"))
           :case-id case-id
           :asset-id asset-id
           :document-id filename
           :user user})))
    {:status 204}))




(defn routes []
  ["/case/:case-id/"
   ["utility" {:post {:handler add-bill
                      :parameters {:body bill-creation-schema}}}]
   ["delete-utility/:bill-id" {:post {:handler delete-bill}}]
   ["update-utility/:bill-id" {:post {:handler update-bill}}]

   ["council-tax" {:post {:handler add-council-tax
                          :parameters {:body council-tax-creation-schema}}}]
   ["council-tax/document/:asset-id" {:post {:handler (partial upload-document :council-tax)}}]
   ["delete-council-tax/:council-tax-id" {:post {:handler delete-bill}}]
   ["update-council-tax/:council-tax-id" {:post {:handler update-bill}}]])

(comment
  (require 'darbylaw.xtdb-node)
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull doc [*])]
      :where [[doc :type :probate.bill]]}))