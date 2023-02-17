(ns darbylaw.api.bill.data
  (:require [darbylaw.api.util.malli :as malli+]))

(def bill-types
  (array-map
    :gas {:label "gas"}
    :electricity {:label "electricity"}
    :telephone {:label "telephone"}
    :broadband {:label "broadband"}
    :water {:label "water"}
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
  [{:common-name "Adur and Worthing Borough Council"
    :address
    "The Shoreham Centre,
    West Sussex,
    BN43 5WU"
    :id :adur-and-worthing}
   {:common-name "Fareham Borough Council"
    :address
    "Civic Offices,
    Civic Way,
    Fareham,
    PO16 7AZ"
    :id :fareham}
   {:common-name "Kettering Borough Council"
    :address
    "Municipal Offices
    Bowling Green Road
    Kettering
    NN15 7QX"
    :id :kettering}
   {:common-name "Pembrokeshire County Council"
    :address
    "County Hall,
    Haverfordwest,
    Pembrokeshire,
    SA61 1TP"
    :id :pembrokeshire}
   {:common-name "Uttlesford District Council"
    :address
    "Council Offices
    London Road
    Saffron Walden
    CB11 4ER"
    :id :uttlesford}])

(defn make-bill-schema [op]
  [:and
   [:map
    [:bill-type [:set (into [:enum] (keys bill-types))]]
    [:issuer {:optional true} (into [:enum] (map :id (concat companies
                                                       councils)))]
    [:custom-issuer-name {:optional true} [:string {:min 1}]]
    [:custom-issuer-address {:optional true} [:string {:min 1}]]
    [:amount [:re #"-?\d*\.?\d{0,2}"]]
    [:account-number {:optional true} :string]
    (if (= op :create)
      [:property [:or :uuid :string]]
      [:property :uuid])
    [:meter-readings {:optional true} :string]]
   (malli+/exclusive-keys [:issuer :custom-issuer-name])
   (malli+/when-then :custom-issuer-name :custom-issuer-address)])

(defn make-council-tax-schema [op]
  [:and
   [:map
    [:bill-type [:set (into [:enum] (keys bill-types))]]
    [:council (into [:enum] (map :id councils))]
    [:account-number {:optional true} :string]
    (if (= op :create)
      [:property [:or :uuid :string]]
      [:property :uuid])]])

(defn extract-bill-props [bill-schema]
  (assert (= :map (-> bill-schema second first)))
  (->> bill-schema
    second                                                  ; skip :and
    rest                                                    ; skip :map
    (mapv first)))

(defn extract-council-tax-props [council-tax-schema]
  (assert (= :map (-> council-tax-schema second first)))
  (->> council-tax-schema
    second                                                  ; skip :and
    rest                                                    ; skip :map
    (mapv first)))

(def company-by-id
  (into {} (map (juxt :id identity) companies)))

(defn get-company-info [company-id]
  (get company-by-id company-id))

(def council-by-id
  (into {} (map (juxt :id identity) councils)))

(defn get-council-info [council-id]
  (get council-by-id council-id))
