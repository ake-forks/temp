(ns darbylaw.api.bank-notification-template
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [xtdb.api :as xt]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.api.bank-list :as banks]
            [stencil.api :as stencil]
            [clojure.java.io :as io]))

(defn letter-template-query [case-id bank-id]
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

(defn get-letter-template-data [xtdb-node case-id bank-id]
  (let [[case-data bank-data] (xt-util/fetch-one
                                (apply xt/q (xt/db xtdb-node)
                                  (letter-template-query case-id bank-id)))]
    (data-util/keys-to-camel-case
      (-> case-data
        (assoc :bank (-> bank-data
                       (assoc :name (banks/bank-label bank-id))))))))

(def bank-notification-template
  (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx")))

(defn render-docx [template-data file]
  (stencil/render! bank-notification-template template-data
    :output file
    :overwrite? true))
