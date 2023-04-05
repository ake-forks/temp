(ns darbylaw.api.case
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]
            [ring.util.response :as ring]
            [darbylaw.api.util.http :as http]
            [darbylaw.api.case-history :as case-history]
            [darbylaw.api.util.xtdb :as xt-util]
            [darbylaw.api.util.tx-fns :as tx-fns]
            [darbylaw.api.bill.data :as bill-data]
            [darbylaw.api.vehicle.data :as vehicle-data]))

(def date--schema
  [:re #"^\d{4}-\d{2}-\d{2}$"])

(def personal-representative--schema
  [:map
   [:title :string]
   [:forename :string]
   [:middlename {:optional true} :string]
   [:surname :string]
   [:gender [:enum "male" "female"]]
   [:date-of-birth date--schema]

   [:email :string]
   [:phone :string]

   [:flat {:optional true} :string]
   [:building :string]
   [:street1 :string]
   [:street2 {:optional true} :string]
   [:town :string]
   [:region {:optional true} :string]
   [:postcode :string]])

(def personal-representative--props
  (mapv first (drop 1 personal-representative--schema)))

(def deceased--schema
  [:map
   [:relationship :string]

   [:registration-district :string]
   [:certificate-number :string]
   [:entry-number :string]

   [:date-of-death date--schema]
   [:place-of-death :string]

   [:forename :string]
   [:surname :string]
   [:sex [:enum "male" "female"]]
   [:maiden-name {:optional true} :string]
   [:date-of-birth date--schema]
   [:place-of-birth :string]
   [:occupation :string]
   [:name-of-informant :string]

   [:cause-of-death :string]
   [:name-of-doctor-certifying :string]
   [:name-of-registrar :string]])

(defn initialize-case [case-id pr-id fake?]
  (tx-fns/invoke ::initialize-case [case-id pr-id fake?]
    '(fn [ctx case-id pr-id fake?]
       (let [db (xtdb.api/db ctx)
             last-case-ref (xtdb.api/entity db :probate/last-case-reference)
             new-case-ref (if (nil? last-case-ref)
                            1
                            (inc (:value last-case-ref)))
             ref-suffix (if fake? "99" "00")
             ;; Pad `new-case-ref` so that it's at least 6 digits total
             reference (str (format "%04d" new-case-ref) ref-suffix)]
         ;; Update/initialise the last-case-ref
         [[::xt/put {:xt/id :probate/last-case-reference
                     :value new-case-ref}]
          ;; Initialise the probate-case
          [::xt/put {:type :probate.case
                     :xt/id case-id
                     :probate.case/personal-representative pr-id
                     :fake fake?
                     :reference reference}]]))))

(defn create-case [{:keys [xtdb-node user body-params]}]
  (let [case-id (random-uuid)
        pr-id (random-uuid)
        pr-data (get body-params :personal-representative)
        fake? (get body-params :fake)]
    (xt-util/exec-tx xtdb-node
      (concat
        (initialize-case case-id pr-id fake?)
        [[::xt/put (merge pr-data
                     {:type :probate.personal-representative
                      :xt/id pr-id})]]
        (case-history/put-event2 {:case-id case-id
                                  :user user
                                  :subject :probate.case
                                  :op :created
                                  :fake fake?})))
    {:status 200
     :body {:id case-id}}))

(defn update-deceased [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        deceased-data body-params
        deceased-id {:probate.deceased/case case-id}]
    (xt-util/exec-tx xtdb-node
      (concat
        [[::xt/put (merge deceased-data
                     deceased-id
                     {:xt/id deceased-id
                      :type :probate.deceased})]]
        (case-history/put-event2 {:case-id case-id
                                  :user user
                                  :subject :probate.case.deceased
                                  :op :updated})))
    {:status 200
     :body deceased-data}))

(defn update-ref [eid ref-k m]
  (tx-fns/invoke ::update-ref [eid ref-k m]
    '(fn [ctx eid ref-k m]
       (let [db (xtdb.api/db ctx)
             entity (xtdb.api/entity db eid)]
         (assert entity (str "entity not found: " eid))
         (let [refed-eid (get entity ref-k)
               _ (assert refed-eid (str "no ref in entity: " ref-k))
               refed-entity (xtdb.api/entity db refed-eid)]
           (assert refed-entity (str "refed entity not found: " refed-eid))
           [[::xt/put (merge m (select-keys refed-entity [:xt/id :type]))]])))))

(defn update-pr-info [{:keys [xtdb-node user path-params body-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        pr-info body-params]
    (xt-util/exec-tx xtdb-node
      (concat
        (update-ref case-id :probate.case/personal-representative pr-info)
        (case-history/put-event2 {:case-id case-id
                                  :user user
                                  :subject :probate.case.personal-representative
                                  :op :updated})))
    {:status 200
     :body pr-info}))

(def document-props
  ['(:xt/id {:as :filename})
   :uploaded-by
   :original-filename])

(def letter-props
  [{:notification-letter ['(:xt/id {:as :id})
                          :author
                          :by
                          :sent-by
                          :sent-at]}
   {:valuation-letter ['(:xt/id {:as :id})
                       :uploaded-by
                       :uploaded-at]}])

(def bill-props
  ['(:xt/id {:as :filename})
   :original-filename
   :uploaded-by
   :uploaded-at])

(def check-props
  [:ssid
   :status
   :result
   :report
   :links-self])

(def common-case-eql
  ['(:xt/id {:as :id})
   :reference
   :fake
   {'(:probate.case/personal-representative {:as :personal-representative})
    personal-representative--props}

   {'(:probate.deceased/_case {:as :deceased
                               :cardinality :one})
    ['*]}

   {:death-certificate document-props}
   {:will document-props}
   {:grant-of-probate document-props}

   {'(:notification-process/_case {:as :notification-process})
    ['*]}

   {'(:probate.funeral-account/_case {:as :funeral-account
                                      :cardinality :one})
    [:title
     :value
     :paid-by
     :paid
     {:receipt document-props}
     {:invoice document-props}]}

   {:funeral-expense
    ['(:xt/id {:as :expense-id})
     :title
     :value
     :paid
     :paid-by
     {:receipt document-props}]}

   {:bank-accounts (into
                     [:bank-id
                      :accounts-unknown
                      :accounts]
                     letter-props)}

   {:buildsoc-accounts (into
                         [:buildsoc-id
                          :accounts-unknown
                          :accounts]
                         letter-props)}

   {'(:probate.council-tax/_case {:as :council-tax})
    (into
      ['(:xt/id {:as :id})
       {'(:probate.council-tax-bill/_council-tax {:as :recent-bill}) bill-props}]
      (bill-data/extract-council-tax-props
        (bill-data/make-council-tax-schema :query)))}

   {'(:probate.utility/_case {:as :utilities})
    (into
      ['(:xt/id {:as :id})
       {'(:probate.utility-bill/_utility {:as :recent-bill}) bill-props}]
      (bill-data/extract-bill-props
        (bill-data/make-bill-schema :query)))}

   {'(:probate.property/_case {:as :properties})
    ['(:xt/id {:as :id})
     :address
     :valuation
     :joint-ownership?
     :joint-owner
     :estimated-value?
     :owned?
     :insured?
     {'(:probate.property-doc/_property
         {:as :documents})
      ['(:xt/id {:as :filename})
       :original-filename
       :uploaded-at
       :uploaded-by]}]}

   {'(:probate.pension/_case {:as :pensions})
    ['(:xt/id {:as :id})
     :provider
     :ni-number
     :reference
     :pension-type
     :start-date
     :tell-us-once
     :valuation]}

   {'(:probate.identity-check.uk-aml/_case
       {:as :uk-aml
        :cardinality :one})
    check-props}
   {'(:probate.identity-check.fraudcheck/_case
       {:as :fraudcheck
        :cardinality :one})
    check-props}
   {'(:probate.identity-check.smartdoc/_case
       {:as :smartdoc
        :cardinality :one})
    check-props}
   :override-identity-check
   {'(:probate.identity-check.note/_case
      {:as :identity-check-note
       :cardinality :one})
    [:note]}
   {:identity-user-docs
    ['(:xt/id {:as :document-id})
     :original-filename
     :uploaded-by]}

   {:vehicles
    (into
      ['(:xt/id {:as :vehicle-id})
       {:documents
        ['(:xt/id {:as :document-id})
         :original-filename
         :uploaded-by]}]
      vehicle-data/props)}])

(defn get-cases [{:keys [xtdb-node]}]
  (ring/response
    (->> (xt/q (xt/db xtdb-node)
           {:find [(list 'pull 'case common-case-eql)]
            :where '[[case :type :probate.case]]})
         (map first))))

(def get-case__query
  {:find [(list 'pull 'case common-case-eql)]
   :where '[[case :type :probate.case]
            [case :xt/id case-id]]
   :in '[case-id]})

(comment
  (xt/entity (xt/db darbylaw.xtdb-node/xtdb-node)
    #uuid"0261303e-0edf-41d8-adb6-caff8d58ea5f")
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node) get-case__query
    #uuid"cd62f859-6b9f-4093-bbb9-7679ad838a62"))

(defn get-case [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        results (xt/q (xt/db xtdb-node) get-case__query case-id)]
    (if (zero? (count results))
      {:status http/status-404-not-found}
      (do
        (assert (= 1 (count results)))
        (-> results
            ffirst
            ring/response)))))

(defn get-case-history [{:keys [xtdb-node path-params]}]
  (let [case-id (parse-uuid (:case-id path-params))
        db (xt/db xtdb-node)
        case-reference (-> (xt/pull db [:reference] case-id)
                         :reference)
        history (->> (xt/q db
                       '{:find [(pull event [(:xt/id {:as :id}) *])
                                timestamp]
                         :where [[event :type :event]
                                 [event :event/case case-id]
                                 [event :timestamp timestamp]]
                         :order-by [[timestamp :desc]]
                         :in [case-id]}
                       case-id)
                  (map first))]
    {:status http/status-200-ok
     :body {:reference case-reference
            :history history}}))

(defn get-event [{:keys [xtdb-node path-params]}]
  (let [event-id (parse-uuid (:event-id path-params))
        event (xt/q (xt/db xtdb-node)
                '{:find [(pull event [:tx-id
                                      :event/case])]
                  :where [[event :type :event]
                          [event :xt/id event-id]]
                  :in [event-id]}
                event-id)
        {:keys [tx-id]
         case-id :event/case} (ffirst event)
        db-before (xt/db xtdb-node {::xt/tx {::xt/tx-id (dec tx-id)}})
        case-before (xt/q db-before get-case__query case-id)
        db-after (xt/db xtdb-node {::xt/tx {::xt/tx-id tx-id}})
        case-after (xt/q db-after get-case__query case-id)]
    (ring/response
      {:case-before (ffirst case-before)
       :case-after (ffirst case-after)})))

(comment
  (require 'darbylaw.xtdb-node)
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node)
    '{:find [(pull event [*])]
      :where [[event :type :event]]})
  (xt/q (xt/db darbylaw.xtdb-node/xtdb-node {::xt/tx {::xt/tx-id -1}})
    '{:find [(pull event [*])]
      :where [[event :type :event]]})
  (xt/submit-tx darbylaw.xtdb-node/xtdb-node
    [[::xt/delete #uuid"b32ecf9a-7f9e-45cd-89f7-3a9abecdfddd"]]))

(defn routes []
  [["/case" {:post {:handler create-case
                    :coercion reitit.coercion.malli/coercion
                    :parameters {:body [:map
                                        [:personal-representative
                                         personal-representative--schema]]}}}]

   ["/case"
    ["/:case-id" {:get {:handler get-case}}]

    ["/:case-id"
     ["/history" {:get {:handler get-case-history}}]

     ["/personal-representative"
      {:put {:handler update-pr-info
             :coercion reitit.coercion.malli/coercion
             :parameters {:body personal-representative--schema}}}]

     ["/deceased"
      {:put {:handler update-deceased
             :coercion reitit.coercion.malli/coercion
             :parameters {:body deceased--schema}}}]]]

   ["/event"
    ["/:event-id" {:get {:handler get-event}}]]

   ["/cases" {:get {:handler get-cases}}]])
