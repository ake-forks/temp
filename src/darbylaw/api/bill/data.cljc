(ns darbylaw.api.bill.data
  (:require [darbylaw.api.util.malli :as malli+]))

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
  [:and
   [:map
    [:bill-type [:set (into [:enum] (keys bill-types))]]

    [:issuer {:optional true} (into [:enum] (map :id companies))]
    [:custom-issuer-name {:optional true} [:string {:min 1}]]
    [:custom-issuer-address {:optional true} [:string {:min 1}]]
    [:amount [:re #"-?\d*\.?\d{0,2}"]]
    [:account-number {:optional true} :string]
    [:address [:or [:enum :deceased-address]
               [:string {:min 1}]]]
    [:meter-readings {:optional true} :string]]
   (malli+/exclusive-keys [:issuer :custom-issuer-name])
   (malli+/when-then :custom-issuer-name :custom-issuer-address)])

(comment
  (require '[malli.core :as malli])
  (malli.core/explain bill-schema {:bill-type #{:other}
                                   ;:issuer :utility-1
                                   :amount "10"
                                   :custom-issuer-name "hola"
                                   :custom-issuer-address "addr"
                                   :address "hey"}))

(def bill-props
  (do
    (assert (= :map (-> bill-schema second first)))
    (->> bill-schema
      second ; skip :and
      rest ; skip :map
      (mapv first))))

(def company-by-id
  (into {} (map (juxt :id identity) companies)))

(defn get-company-info [company-id]
  (get company-by-id company-id))

(def council-by-id
  (into {} (map (juxt :id identity) councils)))

(defn get-council-info [council-id]
  (get council-by-id council-id))
