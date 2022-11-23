(ns darbylaw.api.bank-list)

(def bank-list
  [{:id :aberdeen-standard-investments,
    :common-name "Aberdeen Standard Investments",
    :org-name "Aberdeen Standard Investments"
    :icon "Aberdeen-Standard-Investments-icon.svg"}
   {:id :barclays-bank-plc,
    :common-name "Barclays",
    :org-name "Barclays Bank Plc"
    :icon "Barclays-Bank-icon.svg"}
   {:id :birmingham-midshires,
    :common-name "Birmingham Midshires",
    :org-name "Birmingham Midshires"
    :icon "Birmingham-Midshires-icon.svg"}
   {:id :britannia-bereavement-team,
    :common-name "Britannia",
    :org-name "Britannia Bereavement Team"
    :icon "Britannia-Bereavement-Team.svg"}
   {:id :charter-savings-bank,
    :common-name "Charter Savings Bank",
    :org-name "Charter Savings Bank"
    :icon "Charter-Savings-Bank-icon.svg"}
   {:id :citibank-international-plc,
    :common-name "Citibank",
    :org-name "Citibank International plc"
    :icon "Citibank-International.svg"}
   {:id :coventry-building-society,
    :common-name "Coventry Building Society",
    :org-name "Coventry Building Society"
    :icon "Coventry-Building-Societ-icon.svg"}
   {:id :credit-agricole-cotes-darmor,
    :common-name "Credit Agricole Cotes D'Armor",
    :org-name "Credit Agricole Cotes D'Armor"
    :icon "Credit-Agricole-Cotes-D_Armor.svg"}
   {:id :first-direct,
    :common-name "First Direct",
    :org-name "First Direct"
    :icon "First-Direc-icon.svg"}
   {:id :firstsave,
    :common-name "FirstSave",
    :org-name "FirstSave"
    :icon "FirstSave.svg"}
   {:id :ford-money,
    :common-name "Ford Money",
    :org-name "Ford Money"
    :icon "Ford Money.svg"}
   {:id :halifax-and-hbos,
    :common-name "Halifax",
    :org-name "Halifax and HBOS"
    :icon "Halifax.svg"}
   {:id :hampshire-trust-bank,
    :common-name "Hampshire Trust Bank",
    :org-name "Hampshire Trust Bank"
    :icon "Hampshire-Trust-Bank-icon.svg"}
   {:id :hsbc-bereavement-team,
    :common-name "HSBC",
    :org-name "HSBC Bereavement Team"
    :icon "HSBC-bereavement-team-icon.svg"}
   {:id :kent-reliance,
    :common-name "Kent Reliance",
    :org-name "Kent Reliance"
    :icon "Kent-Reliance.svg"}
   {:id :leeds-building-society,
    :common-name "Leeds Building Society",
    :org-name "Leeds Building Society"
    :icon "Leeds-Building-Society.svg"}
   {:id :lloyds-bank,
    :common-name "Lloyds Bank",
    :org-name "Lloyds Bank"
    :icon "Lloyds-Bank-icon.svg"}
   {:id :lloyds-bank-international,
    :common-name "Lloyds Bank International",
    :org-name "Lloyds Bank International"
    :icon "Lloyds-Bank-icon.svg"}
   {:id :marks-and-spencer-bereavement,
    :common-name "Marks and Spencer Bank",
    :org-name "Marks and Spencer Bereavement"
    :icon "Marks-and-Spencer-Bereavement.svg"}
   {:id :mbna-europe-bank-ltd,
    :common-name "MBNA Europe Bank Ltd",
    :org-name "MBNA Europe Bank Ltd"
    :icon "mbna.svg"}
   {:id :national-savings--investments,
    :common-name "NS&I: National Savings & Investments",
    :org-name "National Savings & Investments"
    :icon "National-Savings-and-Investments.svg"}
   {:id :nationwide-bereavement-services,
    :common-name "Nationwide",
    :org-name "Nationwide Bereavement Services"
    :icon "Nationwide-Bereavement-Services-icon.svg"}
   {:id :natwest,
    :common-name "Natwest",
    :org-name "Natwest"
    :icon "NatWest-icon.svg"}
   {:id :paragon-bank-plc,
    :common-name "Paragon Bank PLC",
    :org-name "Paragon Bank PLC"
    :icon "Paragon-Bank-icon.svg"}
   {:id :post-office-money-savings,
    :common-name "Post Office Savings",
    :org-name "Post Office Money Savings"
    :icon "Post-Office-Account.svg"}
   {:id :rbs-bereavement-service-team,
    :common-name "Royal Bank of Scotland",
    :org-name "RBS Bereavement Service Team"
    :icon "RBS-Bereavement-Service-Team-icon.svg"}
   {:id :saga,
    :common-name "Saga",
    :org-name "Saga"
    :icon "SAGA.svg"}
   {:id :sainsburys-bank,
    :common-name "Sainsbury's Bank",
    :org-name "Sainsbury's Bank"
    :icon "Sainsbury_s-Bank-icon.svg"}
   {:id :santander,
    :common-name "Santander",
    :org-name "Santander"
    :icon "Santander-icon.svg"}
   {:id :skipton-building-society,
    :common-name "Skipton Building Society",
    :org-name "Skipton Building Society"
    :icon "Skipton-Building-Society.svg"}
   {:id :tesco-bank,
    :common-name "Tesco Bank",
    :org-name "Tesco Bank"
    :icon "Tesco-Bank-stacked.svg"}
   {:id :the-cooperative-bank,
    :common-name "The Co-operative Bank",
    :org-name "The Co-operative Bank"
    :icon "Co-operative-Bank-stack.svg"}
   {:id :tsb-estate-settlement-unit,
    :common-name "TSB Bank",
    :org-name "TSB Estate Settlement Unit"
    :icon "TSB-Estate-Settlement-Unit.svg"}
   {:id :virgin-money,
    :common-name "Virgin Money",
    :org-name "Virgin Money"
    :icon "Virgin-Mone-stacked.svg"}
   {:id :yorkshire-building-society,
    :common-name "Yorkshire Building Society",
    :org-name "Yorkshire Building Society"
    :icon "Yorkshire-Building-Societ-icon.svg"}])

(def bank-by-id
  (into {} (map (juxt :id identity) bank-list)))

(defn all-bank-ids []
  (map :id bank-list))

(defn bank-label [bank-id]
  (get-in bank-by-id [bank-id :common-name]))
