(ns darbylaw.api.bill.data)

(def bill-types
  (array-map
    :gas {:label "gas"}
    :electricity {:label "electricity"}
    :telephone {:label "telephone"}
    :broadband {:label "broadband"}
    :council-tax {:label "council tax"}
    :other {:label "other"}))

(def companies
  [{:id :utility-1,
    :common-name "Utility 1",
    :org-name "Utility 1 Org",
    :address-street-no "",
    :address-house-name "",
    :address-line-1 "PO Box 12233",
    :address-line-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil},
   {:id :utility-2,
    :common-name "Utility 2",
    :org-name "Utility 2 Org",
    :address-street-no "",
    :address-house-name "",
    :address-line-1 "PO Box 12233",
    :address-line-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil}])

(def councils
  [{:id :council-1,
    :common-name "Council 1",
    :org-name "Council 1 Org",
    :address-street-no "",
    :address-house-name "",
    :address-line-1 "PO Box 12233",
    :address-line-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil},
   {:id :council-2,
    :common-name "Council 2",
    :org-name "Council 2 Org",
    :address-street-no "",
    :address-house-name "",
    :address-line-1 "PO Box 12233",
    :address-line-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil}])

(def bill-schema
  [:map
   [:bill-type [:set (into [:enum] (keys bill-types))]]
   [:company {:optional true} [:or
                               (into [:enum] (map :id companies))
                               [:string {:min 1}]]]
   [:council {:optional true} [:or
                               (into [:enum] (map :id companies))
                               [:string {:min 1}]]]
   [:amount :string]
   [:account-number {:optional true} :string]
   [:address [:or [:enum :deceased-address]
                  [:string {:min 1}]]]
   [:meter-readings {:optional true} :string]])

(comment
  (require '[malli.core :as malli])
  (malli/explain bill-schema {:company :mine
                              :bill-type #{:ok}}))

(def bill-props
  (->> (rest bill-schema)
    (mapv first)))

(def company-by-id
  (into {} (map (juxt :id identity) companies)))

(defn get-company-info [company-id]
  (get company-by-id company-id))

(def council-by-id
  (into {} (map (juxt :id identity) councils)))

(defn get-council-info [council-id]
  (get council-by-id council-id))
