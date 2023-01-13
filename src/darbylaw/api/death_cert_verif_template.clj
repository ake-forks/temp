(ns darbylaw.api.death-cert-verif-template
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [mount.core :as mount]
            [xtdb.api :as xt]
            [darbylaw.api.util.data :as data-util]
            [darbylaw.api.bank-list :as banks]
            [stencil.api :as stencil]
            [clojure.java.io :as io]))

(defn death-certificate-verification-form-query
  [case-id]
  [{:find ['(pull case [{(:probate.deceased/_case 
                           {:as :deceased.info
                            :cardinality :one})
                         [*]}])]
    :where '[[case :type :probate.case]
             [case :xt/id case-id]]
    :in '[case-id]}
   case-id])


(defn format-date [date]
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "dd MMMM yyyy")]
    (.format date formatter)))

(defn get-letter-template-data [xtdb-node case-id]
  (let [[case-data]
        (->> (death-certificate-verification-form-query case-id)
             (apply xt/q (xt/db xtdb-node))
             xt-util/fetch-one)
        today (format-date (java.time.LocalDate/now))]
    (-> case-data
        (assoc :today today)
        data-util/keys-to-camel-case)))

(mount/defstate death-certificate-verification-form-template
  :start
  (stencil/prepare
    (io/resource
      "darbylaw/templates/death-certificate-verification-form.docx")))

(defn render-docx [template-data file]
  (stencil/render!
    death-certificate-verification-form-template
    template-data
    :output file
    :overwrite? true))

(comment
  (get-letter-template-data
    darbylaw.xtdb-node/xtdb-node
    (parse-uuid "34a6ff1a-c680-4b51-97f7-f14cebc1fc74"))

  (defn open-file [file]
    ;; MacOS & linux only
    (-> (Runtime/getRuntime)
        (.exec (into-array ["/usr/bin/open"
                            (.getAbsolutePath file)]))))
  (def docx-file (files-util/create-temp-file "output" ".docx"))
  (def pdf-file (files-util/create-temp-file "output" ".pdf"))
  (def combined-file (files-util/create-temp-file "combined" ".pdf"))
  (do
    (require '[darbylaw.api.util.files :as files-util]
             '[darbylaw.api.pdf :as pdf]
             '[clj-pdf.core :as clj-pdf])
    (println (.getPath docx-file))
    (println (.getPath pdf-file))
    (let [xtdb-node darbylaw.xtdb-node/xtdb-node
          case-id (parse-uuid "34a6ff1a-c680-4b51-97f7-f14cebc1fc74")
          template-data (get-letter-template-data xtdb-node case-id)]
      (render-docx
        template-data
        docx-file)
      (pdf/convert-file docx-file pdf-file)
      (open-file pdf-file)
      #_
      (clj-pdf/collate (java.io.FileOutputStream. combined-file)
                       (clojure.java.io/input-stream pdf-file)
                       (clojure.java.io/input-stream pdf-file))
      #_
      (open-file combined-file))))
