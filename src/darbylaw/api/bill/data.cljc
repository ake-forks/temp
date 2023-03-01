(ns darbylaw.api.bill.data
  (:require [darbylaw.api.util.malli :as malli+]
            [darbylaw.api.bill.council-data :as councils]))

(def bill-types
  (array-map
    :gas {:label "gas"}
    :electricity {:label "electricity"}
    :telephone {:label "telephone"}
    :broadband {:label "broadband"}
    :council-tax {:label "council tax"}
    :other {:label "other"}))

(def test-companies
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

(def companies
  [{:id :affect-energy, :common-name "Affect Energy"}
   {:id :boost-energy, :common-name "Boost Energy"}
   {:id :british-gas, :common-name "British Gas"}
   {:id :bulb-energy, :common-name "Bulb Energy"}
   {:id :cooperative-energy, :common-name "Co-Operative Energy"}
   {:id :e, :common-name "E"}
   {:id :eon, :common-name "E.ON"}
   {:id :ebico, :common-name "EBICo"}
   {:id :ecotricity, :common-name "Ecotricity"}
   {:id :edf-energy, :common-name "EDF Energy"}
   {:id :good-energy, :common-name "Good Energy"}
   {:id :green-energy-uk, :common-name "Green Energy UK"}
   {:id :ms-energy, :common-name "M&S Energy"}
   {:id :octopus-energy, :common-name "Octopus Energy"}
   {:id :outfox-the-market, :common-name "Outfox the Market"}
   {:id :ovo-energy, :common-name "Ovo Energy"}
   {:id :sainsburys-energy, :common-name "Sainsbury's Energy"}
   {:id :scottish-power, :common-name "Scottish Power"}
   {:id :shell-energy, :common-name "Shell Energy"}
   {:id :so-energy, :common-name "So Energy"}
   {:id :sse, :common-name "SSE"}
   {:id :southern-electric, :common-name "Southern Electric"}
   {:id :scottish-hydro, :common-name "Scottish Hydro"}
   {:id :swalec, :common-name "Swalec"}
   {:id :telecom-utility-warehouse,
    :common-name "Telecom Utility Warehouse"}
   {:id :utilita-energy, :common-name "Utilita Energy"}])

(def test-councils
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

(defn make-bill-schema [op]
  [:and
   [:map
    [:bill-type [:set (into [:enum] (keys bill-types))]]
    [:issuer {:optional true} (into [:enum] (map :id (concat companies
                                                        councils/councils)))]
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

(defn extract-bill-props [bill-schema]
  (assert (= :map (-> bill-schema second first)))
  (->> bill-schema
    second ; skip :and
    rest ; skip :map
    (mapv first)))

(def company-by-id
  (into {} (map (juxt :id identity) companies)))

(defn get-company-info [company-id]
  (get company-by-id company-id))

(def council-by-id
  (into {} (map (juxt :id identity) councils/councils)))

(defn get-council-info [council-id]
  (get council-by-id council-id))
