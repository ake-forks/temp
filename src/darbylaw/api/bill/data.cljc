(ns darbylaw.api.bill.data
  (:require [darbylaw.api.bill.council-data :as councils]))

(def bill-types
  (array-map
    :gas {:label "gas"}
    :electricity {:label "electricity"}
    :telephone {:label "telephone"}
    :broadband {:label "broadband"}
    :water {:label "water"}
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

(def water-companies
  [{:id :affinity-water, :common-name "Affinity Water"}
   {:id :albion-water, :common-name "Albion Water"}
   {:id :anglian-water-services, :common-name "Anglian Water Services"}
   {:id :bristol-water-plc, :common-name "Bristol Water plc"}
   {:id :cambridge-water-company, :common-name "Cambridge Water Company"}
   {:id :icosa-water, :common-name "Icosa Water"}
   {:id :hafren-dyfrdwy-severn-dee,
    :common-name "Hafren Dyfrdwy (Severn Dee)"}
   {:id :dr-cymru-welsh-water, :common-name "Dŵr Cymru Welsh Water"}
   {:id :northern-ireland-water, :common-name "Northern Ireland Water"}
   {:id :independent-water-networks,
    :common-name "Independent Water Networks"}
   {:id :northumbrian-water, :common-name "Northumbrian Water"}
   {:id :portsmouth-water, :common-name "Portsmouth Water"}
   {:id :severn-trent-water, :common-name "Severn Trent Water"}
   {:id :south-east-water, :common-name "South East Water"}
   {:id :south-staffs-water, :common-name "South Staffs Water"}
   {:id :south-west-water, :common-name "South West Water"}
   {:id :southern-water-services, :common-name "Southern Water Services"}
   {:id :sutton-and-east-surrey-water-ses,
    :common-name "Sutton and East Surrey Water (SES)"}
   {:id :thames-water-utilities, :common-name "Thames Water Utilities"}
   {:id :united-utilities-water, :common-name "United Utilities Water"}
   {:id :veolia-water-outsourcing, :common-name "Veolia Water Outsourcing"}
   {:id :wessex-water-services, :common-name "Wessex Water Services"}
   {:id :yorkshire-water-services, :common-name "Yorkshire Water Services"}
   {:id :leep-utilities, :common-name "Leep Utilities"}
   {:id :uisce-irish-water, :common-name "UISCE Irish Water"}
   {:id :guernsey-water, :common-name "Guernsey Water"}
   {:id :jersey-water, :common-name "Jersey Water"}
   {:id :tideway, :common-name "Tideway"}])

(def telecom-companies
  [{:id :bt, :common-name "BT"}
   {:id :sky, :common-name "Sky"}
   {:id :talktalk, :common-name "TalkTalk"}
   {:id :virgin-media, :common-name "Virgin Media"}
   {:id :vodafone, :common-name "Vodafone"}
   {:id :community-fibre, :common-name "Community Fibre"}
   {:id :ee, :common-name "EE"}
   {:id :g-network, :common-name "G Network"}
   {:id :kcom, :common-name "KCOM"}
   {:id :now-broadband, :common-name "Now Broadband"}
   {:id :onestream, :common-name "Onestream"}
   {:id :plusnet, :common-name "Plusnet"}
   {:id :shell-energy-broadband, :common-name "Shell Energy Broadband"}
   {:id :three, :common-name "Three"}
   {:id :trooli, :common-name "Trooli"}
   {:id :truespeed, :common-name "Truespeed"}
   {:id :direct-save-telecom, :common-name "Direct Save Telecom"}])

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
    [:utility-company {:optional true} :keyword]
    [:new-utility-company? {:optional true} :boolean]
    [:account-number {:optional true} :string]
    [:valuation {:optional true} :string]
    (if (= op :create)
      [:property [:or :uuid :string]]
      [:property :uuid])
    [:meter-readings {:optional true} :string]]])

(defn make-council-tax-schema [op]
  [:and
   [:map
    [:council (into [:enum] (map :id councils/councils))]
    [:account-number {:optional true} :string]
    [:valuation {:optional true} :string]
    (if (= op :create)
      [:property [:or :uuid :string]]
      [:property :uuid])]])

(defn extract-bill-props [bill-schema]
  (assert (= :map (-> bill-schema second first)))
  (->> bill-schema
    second ; skip :and
    rest ; skip :map
    (mapv first)))

(defn extract-council-tax-props [council-tax-schema]
  (assert (= :map (-> council-tax-schema second first)))
  (->> council-tax-schema
    second ; skip :and
    rest ; skip :map
    (mapv first)))

(def company-by-id
  (into {} (map (juxt :id identity) companies)))

(defn get-company-info [company-id]
  (get company-by-id company-id))

(defn get-company-label [company-id]
  (:common-name (get company-by-id company-id)))

(def council-by-id
  (into {} (map (juxt :id identity) councils/councils)))

(defn get-council-info [council-id]
  (get council-by-id council-id))

(defn get-council-label [council-id]
  (:common-name (get council-by-id council-id)))
