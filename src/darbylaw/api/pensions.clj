(ns darbylaw.api.pensions
  (:require
    [clojure.java.io :as io]
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.api.util.dates :as date-util]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [java-time.api :as jt]
    [mount.core :as mount]
    [stencil.api :as stencil]
    [xtdb.api :as xt])
  (:import (java.time LocalDate)))

(defn edit-pension [op {:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pension-id (parse-uuid (:pension-id path-params))]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        (tx-fns/set-values pension-id (case op
                                        :private {:reference (:reference body-params)
                                                  :valuation (:valuation body-params)}
                                        :state {:reference (:reference body-params)
                                                :start-date (:start-date body-params)
                                                :valuation (:valuation body-params)}))
        (case-history/put-event2 (merge {:case-id case-id
                                         :user user
                                         :subject :probate.case.pensions
                                         :op :updated
                                         :pension pension-id
                                         :pension-type op}
                                   (when (= op :private)
                                     {:institution (:provider body-params)})))))
    {:status 204}))

(defn add-pension [op {:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pension-id (random-uuid)
        provider (:provider body-params)]
    (xt-util/exec-tx-or-throw xtdb-node
      (concat
        [[::xt/put (merge body-params
                     {:xt/id pension-id
                      :type :probate.pension
                      :probate.pension/case case-id
                      :pension-type op})]]
        (case-history/put-event2 (merge {:case-id case-id
                                         :user user
                                         :subject :probate.case.pensions
                                         :op :added
                                         :pension pension-id
                                         :pension-type op}
                                   (when (= op :private)
                                     {:institution provider})))))
    {:status 204}))

(defn letter-query [case-id]
  [{:find '[(pull case [:reference
                        :deceased {(:probate.deceased/_case {:as :deceased
                                                             :cardinality :one})
                                   [:forename :surname :date-of-death]}])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]]
    :in '[case-id]}
   case-id])

(defn pension-query [pension-id]
  [{:find '[(pull pension [:reference :ni-number :provider])]
    :where '[[pension :type :probate.pension]
             [pension :xt/id pension-id]]
    :in '[pension-id]}
   pension-id])

(defn get-letter-data [xtdb-node case-id pension-id]
  (let [database (xt/db xtdb-node)
        [case-data] (xt-util/fetch-one
                       (apply xt/q database
                         (letter-query case-id)))
        [pension-data] (xt-util/fetch-one
                          (apply xt/q database
                            (pension-query pension-id)))]
    (data-util/keys-to-camel-case
      (merge
        (-> case-data
          (assoc :date (date-util/long-date (jt/local-date) false))
          (assoc-in [:deceased :date-of-death] (date-util/long-date
                                                 (LocalDate/parse
                                                   (:date-of-death (:deceased case-data)))
                                                 false)))
        {:pension (select-keys pension-data [:reference :ni-number])}
        ;TODO what is the provider name for state pensions?
        ;TODO update provider common-name once we have pension data
        {:org-name (name (or (:provider pension-data) "DWP"))
         :no-address "No address data found. Please download and edit letter before sending."}))))
(mount/defstate templates
  :start {:private (stencil/prepare (io/resource "darbylaw/templates/private-pension-notification.docx"))
          :state (stencil/prepare (io/resource "darbylaw/templates/state-pension-notification.docx"))})

(defn render-docx [bill-type template-data file]
  (stencil/render!
    (get templates bill-type)
    template-data
    :output file
    :overwrite? true))

(def private-schema
  [:map
   [:provider :keyword]
   [:ni-number :string]
   [:reference {:optional true} :string]])

(def state-schema
  [:map
   [:tell-us-once {:optional true} :string]
   [:ni-number :string]
   [:reference {:optional true} :string]
   [:start-date {:optional true} :string]])

(defn routes []
  ["/case/:case-id/pension/"
   ["add-private" {:post {:handler (partial add-pension :private)
                          :parameters {:body private-schema}}}]
   ["add-state" {:post {:handler (partial add-pension :state)
                        :parameters {:body state-schema}}}]
   ["edit-private/:pension-id" {:post {:handler (partial edit-pension :private)
                                       :parameters {:body
                                                    [:map
                                                     [:provider :keyword]
                                                     [:reference {:optional true} :string]
                                                     [:valuation {:optional true} :string]]}}}]
   ["edit-state/:pension-id" {:post {:handler (partial edit-pension :state)
                                     :parameters {:body
                                                  [:map
                                                   [:reference {:optional true} :string]
                                                   [:start-date {:optional true} :string]
                                                   [:valuation {:optional true} :string]]}}}]])