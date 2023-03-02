(ns darbylaw.api.letter
  (:require [clojure.tools.logging :as log]
            [xtdb.api :as xt]
            [darbylaw.doc-store :as doc-store]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.case-history :as case-history]))

(defn try-delete-file [case-id letter-id]
  (try
    (doc-store/delete-case-file case-id letter-id)
    (catch Exception e
      (log/warn e "Could not delete file " letter-id))))

(defn delete-letter [{:keys [xtdb-node user path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        letter-id (:letter-id path-params)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [letter-type (:type (xt/pull (xt/db xtdb-node) [:type] letter-id))]
        (xt-util/exec-tx xtdb-node
          (concat
            [[::xt/delete letter-id]]
            (case-history/put-event
              {:event (case letter-type
                        :probate.notification-letter :notification-letter.deleted
                        :probate.received-letter :received-letter.deleted)
               :case-id case-id
               :user user
               :letter-id letter-id})))

        (case letter-type
          :probate.notification-letter
          (try-delete-file case-id (str letter-id ".docx"))
          (try-delete-file case-id (str letter-id ".pdf"))

          :probate.received-letter
          (try-delete-file case-id letter-id))

        {:status http/status-204-no-content}))))

(defn routes []
  [["/case/:case-id"
    ["/letter/:letter-id"
     {:delete {:handler delete-letter}}]]])
