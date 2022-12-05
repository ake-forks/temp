(ns darbylaw.api.pdf
  (:require [mount.core :as mount])
  (:import (org.jodconverter.local.office LocalOfficeManager ExistingProcessAction)
           (org.jodconverter.local LocalConverter)
           (org.jodconverter.core.document DefaultDocumentFormatRegistry)
           (java.io InputStream OutputStream File)
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
  (-> (LocalConverter/make office-manager)
    (.convert docx-in)
    (.as DefaultDocumentFormatRegistry/DOCX)
    (.to pdf-out)
    (.as DefaultDocumentFormatRegistry/PDF)
    (.execute)))

(defn convert-file [^File docx-in ^File pdf-out]
  (-> (LocalConverter/make office-manager)
    (.convert docx-in)
    (.as DefaultDocumentFormatRegistry/DOCX)
    (.to pdf-out)
    (.as DefaultDocumentFormatRegistry/PDF)
    (.execute)))

(comment
  (with-open [in (clojure.java.io/input-stream
                   (clojure.java.io/resource "darbylaw/templates/bank-notification.docx"))
              out (clojure.java.io/output-stream "test.pdf")]
    (convert in out))

  (convert-file
    (File. "bank-notification.docx")
    (File. "test.pdf")))


;; Error seen in development after having the server running in the REPL running for a while.
;; Not sure if it may happen in production:
;
;Execution error (IOException) at com.sun.star.lib.uno.environments.remote.Job/remoteUnoRequestRaisedException (Job.java:173).
;SfxBaseModel::impl_store <file:///home/egg/src/juxt/darbylaw/test.pdf> failed: 0xc10(Error Area:Io Class:Write Code:16) at /build/libreoffice-fresh/src/libreoffice-7.4.2.3/sfx2/source/doc/sfxbasemodel.cxx:3207 at /build/libreoffice-fresh/src/libreoffice-7.4.2.3/sfx2/source/doc/sfxbasemodel.cxx:1783
;java.lang.Thread.run                          Thread.java:  829
;java.util.concurrent.ThreadPoolExecutor$Worker.run              ThreadPoolExecutor.java:  628
;java.util.concurrent.ThreadPoolExecutor.runWorker              ThreadPoolExecutor.java: 1128
;java.util.concurrent.FutureTask.run                       FutureTask.java
;java.util.concurrent.FutureTask.run$$$capture                      FutureTask.java:  264
;org.jodconverter.core.office.AbstractOfficeManagerPoolEntry.lambda$execute$0  AbstractOfficeManagerPoolEntry.java:   80
;org.jodconverter.local.office.LocalOfficeManagerPoolEntry.doExecute     LocalOfficeManagerPoolEntry.java:  120
;org.jodconverter.local.task.LocalConversionTask.execute             LocalConversionTask.java:  121
;org.jodconverter.local.task.LocalConversionTask.storeDocument             LocalConversionTask.java:  182
;com.sun.proxy.$Proxy35.storeToURL
;com.sun.star.lib.uno.bridges.java_remote.ProxyFactory$Handler.invoke                    ProxyFactory.java:  128
;com.sun.star.lib.uno.bridges.java_remote.ProxyFactory$Handler.request                    ProxyFactory.java:  146
;com.sun.star.lib.uno.bridges.java_remote.java_remote_bridge.sendRequest              java_remote_bridge.java:  636
;com.sun.star.lib.uno.environments.remote.JavaThreadPool.enter                  JavaThreadPool.java:   87
;com.sun.star.lib.uno.environments.remote.JobQueue.enter                        JobQueue.java:  303
;com.sun.star.lib.uno.environments.remote.JobQueue.enter                        JobQueue.java:  334
;com.sun.star.lib.uno.environments.remote.Job.execute                             Job.java:  139
;com.sun.star.lib.uno.environments.remote.Job.remoteUnoRequestRaisedException                             Job.java:  173
;com.sun.star.io.IOException: SfxBaseModel::impl_store <file:///home/egg/src/juxt/darbylaw/test.pdf> failed: 0xc10(Error Area:Io Class:Write Code:16) at /build/libreoffice-fresh/src/libreoffice-7.4.2.3/sfx2/source/doc/sfxbasemodel.cxx:3207 at /build/libreoffice-fresh/src/libreoffice-7.4.2.3/sfx2/source/doc/sfxbasemodel.cxx:1783
;org.jodconverter.core.office.OfficeException: Could not store document: test.pdf