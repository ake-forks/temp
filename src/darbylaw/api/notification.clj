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

(defn get-notification-props [{:keys [notification-type] :as params}]
  (case notification-type
    :utility (select-mandatory params [:notification-type
                                       :utility-company
                                       :property])))

(defn start-notification-process [{:keys [xtdb-node body-params user] :as req}]
  (let [case-id (get-in req [:parameters :path :case-id])
        notification-props (get-notification-props body-params)
        id (merge notification-props
                  {:notification-process/case case-id})]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/put (merge {:xt/id id}
                          id)]]
        (tx-fns/set-value id [:ready-to-start] true)
        (case-history/put-event
          (merge notification-props
                 {:event :notification-process.ready-to-start
                  :case-id case-id
                  :user user}))))
    {:status http/status-204-no-content}))

(comment
  (require '[darbylaw.xtdb-node])
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/delete {:notification-type :utility,
                   :utility-company :utility-2,
                   :property #uuid"6573e496-38d6-404c-82fd-3041b5fa97a6",
                   :notification-process/case #uuid"bedfff3f-44ee-4a97-99cd-cfb4c39cdbb7"}]])

  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull notif [*])]
      :where [[notif :notification-process/case]]}))

(defn get-conversation [{:keys [xtdb-node body-params] :as req}]
  (let [case-id (get-in req [:parameters :path :case-id])
        notification-props (get-notification-props body-params)
        resp (->> (xt/q (xt/db xtdb-node)
                    {:find '[(pull letter [*]) modified-at]
                     :where (into [['letter :type :probate.notification-letter]
                                   ['letter :probate.notification-letter/case case-id]
                                   ['letter :modified-at 'modified-at]]
                              (for [[k v] notification-props]
                                ['letter k v]))
                     :order-by [['modified-at :desc]]})
               (map first))]
    {:status http/status-200-ok
     :body resp}))

(defn routes []
  [["/case/:case-id"
    {:parameters {:path [:map [:case-id :uuid]]}}
    ["/start-notification-process"
     {:post {:handler start-notification-process}}]
    ["/conversation"
     {:post {:handler get-conversation}}]]])
