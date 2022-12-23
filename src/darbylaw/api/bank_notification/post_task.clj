(ns darbylaw.api.bank-notification.post-task
  (:require [xtdb.api :as xt]
            [darbylaw.api.util.xtdb :as xt-util]
            [clojure.tools.logging :as log]
            [darbylaw.api.bank-notification.letter-store :as letter-store]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.files :as files-util]
            [darbylaw.api.services.post :as post]
            [mount.core :as mount]
            [darbylaw.xtdb-node :refer [xtdb-node]]
            [darbylaw.api.settings :as settings]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.util.http :as http])
  (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit ScheduledFuture)
           (clojure.lang ExceptionInfo)))

(defn disabled? [xtdb-node]
  (let [disabled? (-> (settings/fetch-settings xtdb-node)
                    :post-letters-disabled?)]
    (when disabled?
      (log/info "Posting letters has been disabled."))
    disabled?))

(defn upload-mail!* [xtdb-node]
  (let [letters (->> (xt/q (xt/db xtdb-node)
                       '{:find [(pull letter [*])]
                         :where [[letter :type :probate.bank-notification-letter]
                                 [letter :approved]
                                 (not [letter :upload-state])]})
                  (map first))]
    (when (and (not (disabled? xtdb-node))
               (seq letters)
               (doc-store/available?)
               (post/available?))
      (doseq [letter-data letters]
        (let [{:keys [case-id bank-id]
               letter-id :xt/id} letter-data
              tx (xt-util/exec-tx xtdb-node
                   (concat
                     [[::xt/match (:xt/id letter-data) letter-data]]
                     (tx-fns/set-value letter-id [:upload-state] :uploading)))
              own? (xt/tx-committed? xtdb-node tx)]
          (when own?
            (try
              (let [temp-file (files-util/create-temp-file letter-id ".pdf")]
                (try
                  (doc-store/fetch-to-file
                    (letter-store/s3-key case-id bank-id letter-id ".pdf")
                    temp-file)
                  (let [remote-filename (str letter-id ".pdf")]
                    (post/post-letter (.getCanonicalPath temp-file) remote-filename)
                    (log/debug "Uploaded file for posting: " remote-filename))
                  (xt-util/exec-tx xtdb-node
                    (tx-fns/set-value letter-id [:upload-state] :uploaded))
                  (catch ExceptionInfo exc
                    (if (= (-> exc ex-data :error) ::doc-store/not-found)
                      (xt-util/exec-tx xtdb-node
                        (tx-fns/set-value letter-id [:upload-state] :not-found))
                      (throw exc)))
                  (finally
                    (.delete temp-file))))
              (catch Exception exc
                (log/warn exc "Error while uploading letter; will retry later.")
                (xt-util/exec-tx xtdb-node
                  (tx-fns/set-value letter-id [:upload-state] :retry-upload))))))))))

(def upload-mail-ongoing?
  (atom false))

(defn upload-mail! [xtdb-node]
  (log/debug "upload-mail! starts")
  (when (compare-and-set! upload-mail-ongoing? false true)
    (try
      (upload-mail!* xtdb-node)
      (finally
        (reset! upload-mail-ongoing? false)
        (log/debug "upload-mail! ends")))))

(def upload-mail-agent
  (agent nil))

(defn post-upload-mail [{:keys [xtdb-node]}]
  (if @upload-mail-ongoing?
    {:status http/status-429-too-many-requests}
    (do
      (send-off upload-mail-agent (fn [_]
                                    (upload-mail! xtdb-node)))
      {:status http/status-204-no-content})))

(defn get-mailing-items [{:keys [xtdb-node]}]
  {:status http/status-200-ok
   :body (->> (xt/q (xt/db xtdb-node)
                '{:find [(pull letter [*
                                       ({:case-id [(:xt/id {:as :id})
                                                   :reference]}
                                        {:as :case})])]
                  :where [[letter :type :probate.bank-notification-letter]]})
           (map first))})

(defn routes []
  [["/mailing/run" {:post {:handler post-upload-mail}}]
   ["/mailing/items" {:get {:handler get-mailing-items}}]])

; Periodic execution disabled.

#_(mount/defstate ^ScheduledExecutorService scheduled-executor
    :start (Executors/newSingleThreadScheduledExecutor)
    :stop (.shutdown scheduled-executor))

#_(mount/defstate ^ScheduledFuture post-task
    :start (.scheduleWithFixedDelay scheduled-executor
             (fn [] (upload-mail! xtdb-node))
             10 10 TimeUnit/SECONDS)
    :stop (.cancel post-task false))

(comment
  (mount/stop #'post-task)
  (mount/start #'post-task)
  (.isCancelled post-task))

(comment
  (upload-mail! xtdb-node))
