(ns darbylaw.api.util.pdf
  (:import [org.apache.pdfbox.multipdf PDFMergerUtility]))

;; https://github.com/dotemacs/pdfboxing/issues/66
;; A more lenient version of pdf-merge/merge-pdfs
;; Allows files, string or input streams as inputs (relies on type coercion)
;; Allows string or output streams as outputs
(defn merge-pdfs [& {:keys [input output]}]
  (let [merger (PDFMergerUtility.)]
    (doseq [source input]
      (.addSource merger source))
    (cond
      (instance? java.io.OutputStream output)
      (.setDestinationStream merger output)

      :else
      (.setDestinationFileName merger output))
    (.mergeDocuments merger)))
