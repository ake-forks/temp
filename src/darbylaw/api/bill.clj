(ns darbylaw.api.bill
  (:require [darbylaw.api.bill.data :as bill-data]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.model :as model]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.files :refer [with-delete]]
            [xtdb.api :as xt]
            [clojure.string :as str]))

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
  (malli.core/explain bill-creation-schema {:services #{:other}
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

(defn add-utility [{:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        utility-id (random-uuid)
        [property-id property-tx] (handle-property args)
        data (-> body-params
               (select-keys creation-props)
               (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        [[::xt/put (merge data
                          {:xt/id utility-id
                           :type :probate.utility
                           :probate.utility/case case-id})]]
        (case-history/put-event {:event :utility-added
                                 :case-id case-id
                                 :user user
                                 :op :add
                                 :event/utility utility-id})))
    {:status 200
     :body {:property property-id
            :utility-company (:utility-company data)}}))

(defn delete-bill [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bill-type (cond
                    (contains? path-params :utility-id) :utility
                    (contains? path-params :council-tax-id) :council-tax
                    :else (assert false))
        bill-id (case bill-type
                  :utility (parse-uuid (:utility-id path-params))
                  :council-tax (parse-uuid (:council-tax-id path-params)))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/delete bill-id]]
        (case-history/put-event {:event (keyword (str bill-type "-deleted"))
                                 :case-id case-id
                                 :user user
                                 :op :delete
                                 (keyword (name :event) (name bill-type)) bill-id}))))
  {:status http/status-204-no-content})

(defn update-bill [bill-type {:keys [xtdb-node user path-params body-params] :as args}]
  (let [case-id (parse-uuid (:case-id path-params))
        [property-id property-tx] (handle-property args)
        bill-id (case bill-type
                  :utility (parse-uuid (:utility-id path-params))
                  :council-tax (parse-uuid (:council-tax-id path-params)))
        bill-data (-> body-params
                    (select-keys (case bill-type
                                   :utility creation-props
                                   :council-tax (council-tax-props :edit)))
                    (assoc :property property-id))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        property-tx
        (tx-fns/set-values bill-id bill-data)
        (case-history/put-event {:event (keyword (str bill-type "-updated"))
                                 :case-id case-id
                                 :user user
                                 :op :update
                                 (keyword (name :event) (name bill-type)) bill-id})))
    {:status 200
     :body {:property property-id
            :utility-company (:utility-company bill-data)}}))

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
                                 :event/council-tax council-tax-id})))
    {:status 200
     :body {:property property-id
            :council (:council council-tax-data)
            :id council-tax-id}}))

(def accepted-filetypes
  #{".pdf" ".png" ".jpeg" ".jpg" ".gif"})

(defn upload-document [asset-type {:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        asset-id (parse-uuid (:asset-id path-params))
        {:keys [tempfile]} (get multipart-params "file")
        reference (model/get-reference xtdb-node case-id)
        orig-filename (get multipart-params "filename")
        extension (data-util/file-extension orig-filename)
        document-id (random-uuid)
        document-type-str (name (case asset-type
                                  :utility :probate.utility-bill
                                  :council-tax :probate.council-tax-bill))
        filename (str reference "." (name asset-type) "." document-id extension)]
    (assert (accepted-filetypes extension))
    (assert (not (str/blank? reference)))
    (with-delete [tempfile tempfile]
      (doc-store/store (str case-id "/" filename) tempfile))
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put {:xt/id filename
                    (keyword document-type-str (name :case)) case-id
                    (keyword document-type-str (name asset-type)) asset-id
                    :uploaded-by (:username user)
                    :uploaded-at (xt-util/now)
                    :original-filename orig-filename}]]
        (case-history/put-event
          {:event (keyword (str document-type-str ".uploaded"))
           :case-id case-id
           :asset-id asset-id
           :document-id filename
           :user user})))
    {:status 204}))

(defn get-document [{:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        filename (:filename path-params)
        input-stream (doc-store/fetch
                       (str case-id "/" filename))]
    {:status 200
     :body input-stream}))


(defn routes []
  ["/case/:case-id/"
   ["utility" {:post {:handler add-utility
                      :parameters {:body bill-creation-schema}}}]
   ["utility/document/:asset-id" {:post {:handler (partial upload-document :utility)}}]
   ["delete-utility/:utility-id" {:post {:handler delete-bill}}]
   ["update-utility/:utility-id" {:post {:handler (partial update-bill :utility)}}]

   ["council-tax" {:post {:handler add-council-tax
                          :parameters {:body council-tax-creation-schema}}}]
   ["council-tax/document/:asset-id" {:post {:handler (partial upload-document :council-tax)}}]
   ["delete-council-tax/:council-tax-id" {:post {:handler delete-bill}}]
   ["update-council-tax/:council-tax-id" {:post {:handler (partial update-bill :council-tax)}}]
   ["household-bills/document/:filename" {:get {:handler get-document}}]])


(comment
  (require 'darbylaw.xtdb-node)

  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull doc [*])]
      :where [[doc :type :probate.utility]]})

  (map first
    (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
      '{:find [(pull e [*]) t]
        :where [[e :type :event]
                [e :event/case #uuid"cd62f859-6b9f-4093-bbb9-7679ad838a62"]
                [e :timestamp t]]
        :order-by [[t :asc]]})))
