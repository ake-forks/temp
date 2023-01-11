(ns darbylaw.api.bank-notification-template
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [mount.core :as mount]
            [xtdb.api :as xt]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.api.bank-list :as banks]
            [stencil.api :as stencil]
            [clojure.java.io :as io]))

(defn bank-letter-template-query [case-id bank-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull bank-accounts [:accounts])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [bank-accounts :type :probate.bank-accounts]
             [bank-accounts :bank-id bank-id]]
    :in '[case-id
          bank-id]}
   case-id
   bank-id])

(defn buildsoc-letter-template-query [case-id bank-id]
  [{:find ['(pull case [:reference
                        :deceased.info])
           '(pull bank-accounts [:accounts])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]
             [bank-accounts :type :probate.buildsoc-accounts]
             [bank-accounts :buildsoc-id bank-id]]
    :in '[case-id
          bank-id]}
   case-id
   bank-id])

(defn get-letter-template-data [xtdb-node bank-type case-id bank-id]
  (let [[case-data bank-data] (xt-util/fetch-one
                                (apply xt/q (xt/db xtdb-node)
                                  (case bank-type
                                    :bank (bank-letter-template-query case-id bank-id)
                                    :buildsoc (buildsoc-letter-template-query case-id bank-id))))]
    (data-util/keys-to-camel-case
      (cond-> case-data
        (= bank-type :bank)
        (assoc :bank (-> bank-data
                       (assoc :name (banks/bank-label bank-id))))))))

(mount/defstate bank-notification-template
  :start (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx")))

(mount/defstate buildsoc-notification-template
  :start (stencil/prepare (io/resource "darbylaw/templates/buildsoc-notification.docx")))

(defn render-docx [bank-type template-data file]
  (stencil/render!
    (case bank-type
      :bank bank-notification-template
      :buildsoc buildsoc-notification-template)
    template-data
    :output file
    :overwrite? true))
