(ns darbylaw.api.notification
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]))

(defn select-mandatory [m ks]
  (doseq [k ks]
    (assert (get m k)
      (str "Missing mandatory key " k)))
  (select-keys m ks))

(defn get-specific-id-props [{:keys [asset-type] :as params}]
  (case asset-type
    :utility-bill (select-mandatory params [:asset-type
                                            :utility-company
                                            :property])))

(defn start-notification-process [{:keys [xtdb-node body-params user] :as req}]
  (let [case-id (get-in req [:parameters :path :case-id])
        specific-id-props (get-specific-id-props body-params)
        id (merge specific-id-props
                  {:notification-process/case case-id})]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/put (merge {:xt/id id}
                          id)]]
        (tx-fns/set-value id [:ready-to-start] true)
        (case-history/put-event
          (merge specific-id-props
                 {:event :notification-process.ready-to-start
                  :case-id case-id
                  :user user}))))
    {:status http/status-204-no-content}))

(comment
  (require '[darbylaw.xtdb-node])
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/delete #uuid"7743bd5c-c98d-49ef-9639-925757a68aea"]])

  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull notif [*])]
      :where [[notif :notification-process/case]]}))

(defn get-conversation [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        resp (->> (xt/q (xt/db xtdb-node)
                    {:find '[(pull letter [*])]
                     :where '[[letter :type :probate.notification-letter]]})
               (map first))]
    {:status http/status-200-ok
     :body resp}))

(defn routes []
  [["/case/:case-id"
    ["/start-notification-process"
     {:post {:handler start-notification-process
             :parameters {:path [:map [:case-id :uuid]]}}}]
    ["/conversation"
     {:post {:handler get-conversation}}]]])
