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
            [darbylaw.api.settings :as settings])
  (:import (java.util.concurrent Executors ScheduledExecutorService TimeUnit ScheduledFuture)
           (clojure.lang ExceptionInfo)))

(defn disabled? [xtdb-node]
  (let [disabled? (-> (settings/fetch-settings xtdb-node)
                    :post-letters-disabled?)]
    (when disabled?
      (log/info "Posting letters has been disabled."))
    disabled?))

(def task-type :probate.bank-notification-post-task)

(defn create-post-task! [xtdb-node case-id bank-id]
  (let [task-id {:type task-type
                 :case-id case-id
                 :bank-id bank-id}
        tx (xt-util/exec-tx xtdb-node
             [[::xt/match task-id nil]
              [::xt/put {:type task-type
                         :xt/id task-id
                         :case-id case-id
                         :bank-id bank-id
                         :post-state :scheduled
                         :created-at (xt-util/now)}]])]
    (xt/tx-committed? xtdb-node tx)))

(defn exec-post-tasks! [xtdb-node]
  (log/debug "exec-post-tasks! starts")
  (let [scheduled-tasks (->> (xt/q (xt/db xtdb-node)
                               '{:find [(pull task [*])]
                                 :where [[task :type task-type]
                                         [task :post-state :scheduled]]
                                 :in [task-type]}
                               task-type)
                          (map first))]
    (when (and (not (disabled? xtdb-node))
               (seq scheduled-tasks)
               (doc-store/available?)
               (post/available?))
      (doseq [{:keys [case-id bank-id] :as task-data} scheduled-tasks]
        (let [tx (xt-util/exec-tx xtdb-node
                   [[::xt/match (:xt/id task-data) task-data]
                    [::xt/put (-> task-data
                                (assoc :post-state :uploading))]])
              own-task? (xt/tx-committed? xtdb-node tx)]
          (when own-task?
            (try
              (let [temp-file (files-util/create-temp-file ".pdf")
                    filename (str "bank-notification." case-id "." (name bank-id) ".pdf")]
                (try
                  (doc-store/fetch-to-file
                    (letter-store/s3-key case-id bank-id ".pdf")
                    temp-file)
                  (post/post-letter (.getCanonicalPath temp-file) filename)
                  (log/debug "Uploaded file for posting: " filename)
                  (xt-util/exec-tx xtdb-node
                    [[::xt/put (-> task-data
                                 (assoc :post-state :uploaded))]])
                  (catch ExceptionInfo exc
                    (if (= (-> exc ex-data :error) ::doc-store/not-found)
                      (xt-util/exec-tx xtdb-node
                        [[::xt/put (-> task-data
                                     (assoc :post-state :failed))]])
                      (throw exc)))
                  (finally
                    (.delete temp-file))))
              (catch Exception exc
                (log/error exc "Error while uploading letter; will retry later.")
                (xt-util/exec-tx xtdb-node
                  [[::xt/put (-> task-data
                               (assoc :post-state :retry-upload))]]))))))))
  (log/debug "exec-post-tasks! ends"))

(mount/defstate ^ScheduledExecutorService scheduled-executor
  :start (Executors/newSingleThreadScheduledExecutor)
  :stop (.shutdown scheduled-executor))

(mount/defstate ^ScheduledFuture post-task
  :start (.scheduleWithFixedDelay scheduled-executor
           (fn [] (exec-post-tasks! xtdb-node))
           10 10 TimeUnit/SECONDS)
  :stop (.cancel post-task false))

(comment
  (mount/stop #'post-task)
  (mount/start #'post-task)
  (.isCancelled post-task))

(comment
  (create-post-task! xtdb-node :dummy-case-id :dummy-bank-id)
  (create-post-task! xtdb-node
    #uuid"041c820a-2e09-4b73-8452-0d0c3feb281b"
    :britannia-bereavement-team)
  (exec-post-tasks! xtdb-node)

  (def task-data
    (xt/q (xt/db xtdb-node)
      '{:find [(pull task [*])]
        :where [[task :type task-type]
                [task :bank-id :britannia-bereavement-team]]
        :in [task-type]}
      task-type))

  (xt-util/exec-tx xtdb-node
    [[::xt/put (-> task-data
                 ffirst
                 (assoc :post-state :scheduled))]])

  ,)