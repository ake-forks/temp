(ns darbylaw.api.notification-process
  (:require
    [darbylaw.api.case-history :as case-history]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.tx-fns :as tx-fns]
    [darbylaw.api.util.xtdb :as xt-util]
    [xtdb.api :as xt]))

; A :notification-process is not a first-level entity. It is an application concept
; that indirectly refers to related assets and :notification-letters by matching data,
; i.e. those referred entities don't have a direct reference to the :notification-process.

(defn select-mandatory [m ks]
  (doseq [k ks]
    (assert (get m k)
      (str "Missing mandatory key " k)))
  (select-keys m ks))

(defn get-notification-props [{:keys [notification-type] :as params}]
  (case notification-type
    :utility (select-mandatory params [:notification-type
                                       :utility-company
                                       :property])
    :council-tax (select-mandatory params [:notification-type
                                           :council
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
        (case-history/put-event2
          (merge notification-props
                 {:case-id case-id
                  :user user
                  :subject :probate.case.notification-process
                  :op :set-ready-to-start}))))
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
                    {:find '[(pull letter [*]) order-date]
                     :in '[case-id]
                     :where (into
                              '[(or (and [letter :probate.notification-letter/case case-id]
                                         [letter :modified-at order-date])
                                    (and [letter :probate.received-letter/case case-id]
                                         [letter :uploaded-at order-date]))]
                              (for [[k v] notification-props]
                                ['letter k v]))
                     :order-by [['order-date :desc]]}
                    case-id)
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
