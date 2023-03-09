(ns darbylaw.api.smart-search.note
  (:require [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.http :as http]))

(defn update-note [{:keys [xtdb-node user parameters]}]
  (let [{:keys [case-id]} (:path parameters)
        {:keys [note]} (:body parameters)
        note-id {:probate.identity-check.note/case case-id}]
    (xt-util/exec-tx xtdb-node
      (concat
        (tx-fns/set-values note-id
                           (merge note-id
                                  {:xt/id note-id
                                   :note note}))
        (case-history/put-event2
          {:case-id case-id
           :user user
           :subject :probate.case.identity-check.note
           :op :added})))
    {:status http/status-204-no-content}))

(comment
  (update-note {:xtdb-node darbylaw.xtdb-node/xtdb-node
                :user {:username "ake"}
                :parameters {:path {:case-id (parse-uuid "125dca09-dfce-4c21-9cd0-a89b103c5031")}
                             :body {:note "Hello World"}}}))

(defn routes []
  ["/note"
   {:post {:handler update-note
           :parameters {:path [:map [:case-id :uuid]]
                        :body [:map [:note :string]]}}}])
