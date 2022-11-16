(ns darbylaw.api.bank-notification
  (:require [clojure.java.io :as io]
            [ring.util.io :as ring-io]
            [stencil.api :as stencil])
  (:import (org.jodconverter.local.office LocalOfficeManager)
           (org.jodconverter.local JodConverter)
           (org.jodconverter.core.document DefaultDocumentFormatRegistry)
           (java.io OutputStream)))

(def office-manager
  (doto (LocalOfficeManager/install)
    (.start)))

(def bank-notification-template
  (stencil/prepare (io/resource "darbylaw/templates/bank-notification.docx")))

(defn get-notification-doc [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        bank-id (:bank-id path-params)]
    {:headers {"Content-Type" "application/pdf"}
     :body (ring-io/piped-input-stream
             (fn [^OutputStream output-stream]
               (-> (JodConverter/convert (io/file "bank-notification-1.docx"))
                 (.to output-stream)
                 (.as DefaultDocumentFormatRegistry/PDF)
                 (.execute))))}))

(defn routes []
  [["/case/:case-id/bank-accounts/:bank-id/notification-doc"
    {:get {:handler get-notification-doc}}]])
