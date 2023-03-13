(ns darbylaw.api.bill.data
  (:require [darbylaw.api.bill.council-data :as councils]))

(def utility-services
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
    :address-1 "PO Box 12233",
    :address-2 "",
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
    :address-1 "PO Box 12233",
    :address-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil}])

(def energy-companies
  [{:id :affect-energy,
    :common-name "Affect Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :boost-energy,
    :common-name "Boost Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :british-gas,
    :common-name "British Gas",
    :address-1 "Millstream",
    :address-2 "Maidenhead Road",
    :town "Windsor",
    :county "",
    :postcode "SL4 5GD",
    :logo ""}
   {:id :bulb-energy,
    :common-name "Bulb Energy",
    :address-1 "The Zetland Building",
    :address-2 "Unit B2.01-02 & B2.05-06",
    :town "London ",
    :county "",
    :postcode "SE16 4DG",
    :logo ""}
   {:id :cooperative-energy,
    :common-name "Co-Operative Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :e,
    :common-name "E",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :eon,
    :common-name "E.ON",
    :address-1 "Westwood Way",
    :address-2 "Westwood Business Park",
    :town " Coventry ",
    :county "",
    :postcode "CV4 8LG",
    :logo ""}
   {:id :ebico,
    :common-name "EBICo",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :ecotricity,
    :common-name "Ecotricity",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :edf-energy,
    :common-name "EDF Energy",
    :address-1 "90 Whitfield Street",
    :address-2 "",
    :town " London ",
    :county "",
    :postcode "W1T 4EZ",
    :logo ""}
   {:id :good-energy,
    :common-name "Good Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :green-energy-uk,
    :common-name "Green Energy UK",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :ms-energy,
    :common-name "M&S Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :octopus-energy,
    :common-name "Octopus Energy",
    :address-1 "33 Holborn",
    :address-2 "",
    :town " London ",
    :county "",
    :postcode "EC1N 2HT",
    :logo ""}
   {:id :outfox-the-market,
    :common-name "Outfox the Market",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :ovo-energy,
    :common-name "Ovo Energy",
    :address-1 "1 Rivergate",
    :address-2 "Temple Quay",
    :town "Bristol ",
    :county "",
    :postcode "BS1 6ED",
    :logo ""}
   {:id :sainsburys-energy,
    :common-name "Sainsbury's Energy",
    :address-1 "PO Box 227",
    :address-2 "",
    :town "Rotheram",
    :county "",
    :postcode "S98 1PD",
    :logo ""}
   {:id :scottish-power,
    :common-name "Scottish Power",
    :address-1 "320 St Vincent Street",
    :address-2 "",
    :town "Glasgow ",
    :county "",
    :postcode "G2 5AD",
    :logo ""}
   {:id :shell-energy,
    :common-name "Shell Energy",
    :address-1 "Shell Energy House",
    :address-2 "Westwood Way",
    :town "Westwood Business Park Coventry ",
    :county "",
    :postcode "CV4 8HS",
    :logo ""}
   {:id :so-energy,
    :common-name "So Energy",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :sse,
    :common-name "SSE",
    :address-1 "",
    :address-2 "",
    :town "Inveralmond House, 200 Dunkeld Road Perth ",
    :county "",
    :postcode "PH1 3AQ",
    :logo ""}
   {:id :southern-electric,
    :common-name "Southern Electric",
    :address-1 "",
    :address-2 "",
    :town "PO Box 13     Havant ",
    :county "",
    :postcode "PO9 5JB",
    :logo ""}
   {:id :scottish-hydro,
    :common-name "Scottish Hydro",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :swalec,
    :common-name "Swalec",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :telecom-utility-warehouse,
    :common-name "Telecom Utility Warehouse",
    :address-1 "Network Hq 508 Edgware Road",
    :address-2 "The Hyde",
    :town "London ",
    :county "",
    :postcode "NW9 5AB",
    :logo ""}
   {:id :utilita-energy,
    :common-name "Utilita Energy",
    :address-1 "Hutwood Court",
    :address-2 "Bournemouth Road",
    :town "Chandler’s Ford ",
    :county "Eastleigh",
    :postcode "SO53 3QB",
    :logo ""}])


(def water-companies
  [{:id :affinity-water,
    :common-name "Affinity Water ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :albion-water,
    :common-name "Albion Water ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :anglian-water,
    :common-name "Anglian Water Services ",
    :address-1 "Lancaster House Lancaster Way",
    :address-2 " Ermine Business Park Huntingdon ",
    :town "Huntingdon",
    :county "",
    :postcode "PE29 6XU",
    :logo ""}
   {:id :bristol-water,
    :common-name "Bristol Water plc",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :cambridge-water-company,
    :common-name "Cambridge Water Company ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :icosa-water,
    :common-name "Icosa Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :hafren-dyfrdwy-severn-dee,
    :common-name "Hafren Dyfrdwy (Severn Dee)",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :dr-cymru-welsh-water,
    :common-name "Dŵr Cymru Welsh Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :northern-ireland-water,
    :common-name "Northern Ireland Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :independent-water-networks,
    :common-name "Independent Water Networks ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :northumbrian-water,
    :common-name "Northumbrian Water ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :portsmouth-water,
    :common-name "Portsmouth Water ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :severn-trent-water,
    :common-name "Severn Trent Water ",
    :address-1 "Severn Trent Centre",
    :address-2 " 2 St John’s Street  ",
    :town "Coventry",
    :county "",
    :postcode "CV1 2LZ",
    :logo ""}
   {:id :south-east-water,
    :common-name "South East Water ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :south-staffs-water,
    :common-name "South Staffs Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :south-west-water,
    :common-name "South West Water ",
    :address-1 "Peninsula House",
    :address-2 " Rydon Lane ",
    :town "Exeter",
    :county "",
    :postcode "EX2 7HR",
    :logo ""}
   {:id :pennon-group,
    :common-name "Pennon Group plc",
    :address-1 "Peninsula House",
    :address-2 " Rydon Lane ",
    :town "Exeter",
    :county "",
    :postcode "EX2 7HR",
    :logo ""}
   {:id :southern-water,
    :common-name "Southern Water Services ",
    :address-1 "Southern House",
    :address-2 "Yeoman Road ",
    :town "Worthing",
    :county "",
    :postcode " BN13 3NX",
    :logo ""}
   {:id :sutton-and-east-surrey-water-ses,
    :common-name "Sutton and East Surrey Water (SES)",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :thames-water-utilities,
    :common-name "Thames Water Utilities ",
    :address-1 "Clearwater Court",
    :address-2 "Vastern Road ",
    :town "Reading",
    :county "",
    :postcode "RG1 8DB",
    :logo ""}
   {:id :united-utilities-water,
    :common-name "United Utilities Water ",
    :address-1 "Haweswater House \nLingley Mere Business Park",
    :address-2 " Lingley Green Avenue ",
    :town "Great Sankey",
    :county "Warrington",
    :postcode "WA5 3LP",
    :logo ""}
   {:id :veolia-water-outsourcing,
    :common-name "Veolia Water Outsourcing ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :wessex-water,
    :common-name "Wessex Water Services ",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :yorkshire-water,
    :common-name "Yorkshire Water Services ",
    :address-1 "Western House",
    :address-2 "Halifax Road  ",
    :town "Bradford",
    :county "",
    :postcode "BD6 2SZ",
    :logo ""}
   {:id :leep-utilities,
    :common-name "Leep Utilities",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :uisce-irish-water,
    :common-name "UISCE Irish Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :guernsey-water,
    :common-name "Guernsey Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :jersey-water,
    :common-name "Jersey Water",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :tideway,
    :common-name "Tideway",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}])

(def telecom-companies
  [{:id :bt,
    :common-name "BT",
    :address-1 "BT Centre",
    :address-2 "81 Newgate Street",
    :town "London",
    :county "",
    :postcode "EC1A 7AJ",
    :logo ""}
   {:id :sky,
    :common-name "Sky",
    :address-1 "Grant Way  ",
    :address-2 "",
    :town "Isleworth",
    :county "",
    :postcode "TW7 5QD",
    :logo ""}
   {:id :talktalk,
    :common-name "TalkTalk",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :virgin-media,
    :common-name "Virgin Media",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :vodafone,
    :common-name "Vodafone",
    :address-1 "Vodafone House ",
    :address-2 "The Connection",
    :town "Newbury",
    :county "Berkshire",
    :postcode "RG14 2FN",
    :logo ""}
   {:id :community-fibre,
    :common-name "Community Fibre",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :ee,
    :common-name "EE",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :g-network,
    :common-name "G Network",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :kcom,
    :common-name "KCOM",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :now-broadband,
    :common-name "Now Broadband",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :onestream,
    :common-name "Onestream",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :plusnet,
    :common-name "Plusnet",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :shell-energy-broadband,
    :common-name "Shell Energy Broadband",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :three,
    :common-name "Three",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :trooli,
    :common-name "Trooli",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :truespeed,
    :common-name "Truespeed",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :direct-save-telecom,
    :common-name "Direct Save Telecom",
    :address-1 "",
    :address-2 "",
    :town "",
    :county "",
    :postcode "",
    :logo ""}
   {:id :o2,
    :common-name "O2",
    :address-1 "260 Bath Road ",
    :address-2 "",
    :town "Slough",
    :county "Berkshire",
    :postcode "SL1 4DX",
    :logo ""}])

(def companies
  (concat energy-companies water-companies telecom-companies))

(def test-councils
  [{:id :council-1,
    :common-name "Council 1",
    :org-name "Council 1 Org",
    :address-street-no "",
    :address-house-name "",
    :address-1 "PO Box 12233",
    :address-2 "",
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
    :address-1 "PO Box 12233",
    :address-2 "",
    :address-line-3 "",
    :address-town "None",
    :address-postcode "CM99 2EE",
    :address-county "",
    :address-country "",
    :logo nil}])

(defn make-bill-schema [op]
  [:and
   [:map
    [:services [:set (into [:enum] (keys utility-services))]]
    [:utility-company {:optional true} :keyword]
    [:new-utility-name {:optional true} :string]
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
  (into {} (map (juxt :id identity) councils/council-addresses-e-and-w)))

(defn get-council-info [council-id]
  (get council-by-id council-id))

(defn get-council-label [council-id]
  (:common-name (get council-by-id council-id)))
