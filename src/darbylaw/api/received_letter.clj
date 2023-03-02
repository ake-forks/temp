(ns darbylaw.api.received-letter
  (:require
    [clojure.string :as str]
    [xtdb.api :as xt]
    [darbylaw.doc-store :as doc-store]
    [darbylaw.api.util.xtdb :as xt-util]
    [darbylaw.api.util.http :as http]
    [darbylaw.api.util.files :refer [with-delete]]
    [darbylaw.api.case-history :as case-history]
    [clojure.edn :as edn]))

(defn select-mandatory [m ks]
  (doseq [k ks]
    (assert (get m k)
      (str "Missing mandatory key " k)))
  (select-keys m ks))

(comment
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull e [*])]
      :where [[e :type :probate.notification-letter]]}))

(defn post-received-letter [{:keys [xtdb-node user path-params multipart-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        {:keys [tempfile content-type]} (get multipart-params "file")
        params (-> multipart-params
                 (dissoc "file")
                 (update-keys keyword)
                 (update-vals edn/read-string))
        _ (assert (= content-type http/pdf-mime-type))
        case-data (xt/pull (xt/db xtdb-node) [:reference] case-id)
        notification-type (:notification-type params)
        letter-id (str/join "."
                    [(:reference case-data)
                     "received-letter"
                     (name notification-type)
                     (case notification-type
                       :utility (name (:utility-company params))
                       :council-tax (name (:council params)))
                     (random-uuid)
                     "pdf"])]
    (with-delete [tempfile tempfile]
      (doc-store/store-case-file case-id letter-id tempfile))
    (let [specific-props (case notification-type
                           :utility (select-mandatory params [:utility-company
                                                              :property])
                           :council-tax (select-mandatory params [:council
                                                                  :property]))]
      (xt-util/exec-tx-or-throw xtdb-node
        (concat
          [[::xt/put (merge {:type :probate.received-letter
                             :xt/id letter-id
                             :probate.received-letter/case case-id
                             :by (:username user)
                             :notification-type notification-type
                             :modified-at (xt-util/now)}
                            specific-props)]]
          (case-history/put-event (merge {:event :notification.letter-received
                                          :case-id case-id
                                          :user user
                                          :letter-id letter-id
                                          :notification-type notification-type}
                                         specific-props)))))
    {:status http/status-204-no-content}))

(comment
  (require '[darbylaw.xtdb-node])
  (def all-letters
    (->> (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
           {:find '[(pull letter [*])]
            :where '[[letter :type #{:probate.received-letter
                                     #_:probate.notification-letter}]]})
      (map first)))
  (def all-events
    (->> (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
           {:find '[(pull evt [*])]
            :where '[[evt :event :notification.letter-received]]})
      (map first)))
  (doseq [letter all-letters]
    (xt/submit-tx darbylaw.xtdb-node/xtdb-node
      [[::xt/delete (:xt/id letter)]])))

(defn get-received-letter-pdf [{:keys [path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        letter-id (:letter-id path-params)]
    (if-not letter-id
      {:status http/status-404-not-found}
      (let [input-stream (doc-store/fetch-case-file case-id letter-id)]
        {:status http/status-200-ok
         :headers {"Content-Type" http/pdf-mime-type}
         :body input-stream}))))

(defn routes []
  [["/case/:case-id"
    ["/received-letter"
     {:post {:handler post-received-letter}}]
    ["/received-letter/:letter-id/pdf"
     {:get {:handler get-received-letter-pdf}}]]])
