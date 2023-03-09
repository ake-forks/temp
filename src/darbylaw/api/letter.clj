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
      (let [letter-data (xt/entity (xt/db xtdb-node) letter-id)
            letter-type (:type letter-data)
            notification-type (:notification-type letter-data)]
        (xt-util/exec-tx xtdb-node
          (concat
            [[::xt/delete letter-id]]
            (case-history/put-event2
              {:case-id case-id
               :user user
               :subject (case letter-type
                          :probate.notification-letter :probate.case.notification-letter
                          :probate.received-letter :probate.case.received-letter)
               :op :deleted
               :institution-type notification-type
               :institution (get letter-data (case notification-type
                                               :utility :utility-company
                                               :council-tax :council))
               :letter letter-id})))

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
