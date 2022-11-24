(ns darbylaw.api.pdf
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:import (org.jodconverter.local.office LocalOfficeManager ExistingProcessAction)
           (org.jodconverter.local LocalConverter)
           (org.jodconverter.core.document DefaultDocumentFormatRegistry)
           (java.io InputStream OutputStream)
           (org.jodconverter.core.office OfficeManager)))

(mount/defstate ^OfficeManager office-manager
  :start
  ; See https://github.com/sbraconnier/jodconverter/wiki/Configuration
  (-> (LocalOfficeManager/builder)
      (.startFailFast true)
      (.existingProcessAction ExistingProcessAction/CONNECT_OR_KILL)
      (.processTimeout 10000)
      (.build)
      (doto
        (.start)))

  :stop
  (.stop office-manager))

(defn convert [^InputStream docx-in ^OutputStream pdf-out]
  (try
    (-> (LocalConverter/make office-manager)
      (.convert docx-in)
      (.as DefaultDocumentFormatRegistry/DOCX)
      (.to pdf-out)
      (.as DefaultDocumentFormatRegistry/PDF)
      (.execute))
    (catch Exception e
      (log/error e "PDF conversion failed!"))))

(comment
  (with-open [in (clojure.java.io/input-stream
                   (clojure.java.io/resource "darbylaw/templates/bank-notification.docx"))
              out (clojure.java.io/output-stream "test.pdf")]
    (convert in out)))
