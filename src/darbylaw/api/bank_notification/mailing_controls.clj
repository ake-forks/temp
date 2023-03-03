(ns darbylaw.api.bank-notification.mailing-controls
  (:require
    [clojure.tools.logging :as log]
    [darbylaw.api.util.http :as http]
    [xtdb.api :as xt]
    [darbylaw.api.bank-notification.mailing-fetch :refer [fetch-letters-to-send]]
    [darbylaw.api.bank-notification.mailing-job :refer [upload-mail!]]
    [darbylaw.api.bank-notification.mailing-sync-job :refer [sync!]]))

(defn get-mailing-items [{:keys [xtdb-node]}]
  {:status http/status-200-ok
   :body (->> (xt/q (xt/db xtdb-node)
                '{:find [(pull letter [*
                                       ({:case-id [(:xt/id {:as :id})
                                                   :reference]}
                                        {:as :case})])]
                  :where [[letter :type :probate.bank-notification-letter]]})
           (map first))})

(def running? (atom false))
(def thread-delegation-agent (agent nil))
(defn run-if-not-already-running [f]
  (let [run? (compare-and-set! running? false true)]
    (when run?
      (send-off thread-delegation-agent
        (fn [_]
          (try
            (f)
            (catch Exception e
              (log/error e "Manual mailing operation failed"))
            (finally
              (reset! running? false))))))
    run?))

(comment
  (reset! running? false))

(defn upload-mail-handler [{:keys [xtdb-node]}]
  (let [run? (run-if-not-already-running
               (fn []
                 (log/info "Starting: manual upload-mail! for fake letters")
                 (try
                   (upload-mail! xtdb-node :fake (fetch-letters-to-send xtdb-node :fake))
                   (finally
                     (log/info "Finished: manual upload-mail! for fake letters")))))]
    (if run?
      {:status http/status-204-no-content}
      {:status http/status-429-too-many-requests})))

(defn sync-handler [{:keys [xtdb-node]}]
  (let [run? (run-if-not-already-running
               (fn []
                 (log/info "Starting: manual sync! for fake letters")
                 (try
                   (sync! :fake xtdb-node)
                   (finally
                     (log/info "Finished: manual sync! for fake letters")))))]
    (if run?
      {:status http/status-204-no-content}
      {:status http/status-429-too-many-requests})))

(defn routes []
  [["/mailing/items" {:get {:handler get-mailing-items}}]
   ["/mailing/run" {:post {:handler upload-mail-handler}}]
   ["/mailing/sync" {:post {:handler sync-handler}}]])
