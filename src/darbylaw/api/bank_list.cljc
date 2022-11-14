(ns darbylaw.api.bank-list)

(def bank-list
  [{:id :aberdeen-standard-investments,
    :common-name "Aberdeen Standard Investments",
    :org-name "Aberdeen Standard Investments"}
   {:id :barclays-bank-plc,
    :common-name "Barclays",
    :org-name "Barclays Bank Plc"}
   {:id :birmingham-midshires,
    :common-name "Birmingham Midshires",
    :org-name "Birmingham Midshires"}
   {:id :britannia-bereavement-team,
    :common-name "Britannia",
    :org-name "Britannia Bereavement Team"}
   {:id :charter-savings-bank,
    :common-name "Charter Savings Bank",
    :org-name "Charter Savings Bank"}
   {:id :citibank-international-plc,
    :common-name "Citibank",
    :org-name "Citibank International plc"}
   {:id :coventry-building-society,
    :common-name "Coventry Building Society",
    :org-name "Coventry Building Society"}
   {:id :credit-agricole-cotes-darmor,
    :common-name "Credit Agricole Cotes D'Armor",
    :org-name "Credit Agricole Cotes D'Armor"}
   {:id :first-direct,
    :common-name "First Direct",
    :org-name "First Direct"}
   {:id :firstsave, :common-name "FirstSave", :org-name "FirstSave"}
   {:id :ford-money, :common-name "Ford Money", :org-name "Ford Money"}
   {:id :halifax-and-hbos,
    :common-name "Halifax",
    :org-name "Halifax and HBOS"}
   {:id :hampshire-trust-bank,
    :common-name "Hampshire Trust Bank",
    :org-name "Hampshire Trust Bank"}
   {:id :hsbc-bereavement-team,
    :common-name "HSBC",
    :org-name "HSBC Bereavement Team"}
   {:id :kent-reliance,
    :common-name "Kent Reliance",
    :org-name "Kent Reliance"}
   {:id :leeds-building-society,
    :common-name "Leeds Building Society",
    :org-name "Leeds Building Society"}
   {:id :lloyds-bank, :common-name "Lloyds Bank", :org-name "Lloyds Bank"}
   {:id :lloyds-bank-international,
    :common-name "Lloyds Bank International",
    :org-name "Lloyds Bank International"}
   {:id :marks-and-spencer-bereavement,
    :common-name "Marks and Spencer Bank",
    :org-name "Marks and Spencer Bereavement"}
   {:id :mbna-europe-bank-ltd,
    :common-name "MBNA Europe Bank Ltd",
    :org-name "MBNA Europe Bank Ltd"}
   {:id :national-savings--investments,
    :common-name "NS&I: National Savings & Investments",
    :org-name "National Savings & Investments"}
   {:id :nationwide-bereavement-services,
    :common-name "Nationwide",
    :org-name "Nationwide Bereavement Services"}
   {:id :natwest, :common-name "Natwest", :org-name "Natwest"}
   {:id :paragon-bank-plc,
    :common-name "Paragon Bank PLC",
    :org-name "Paragon Bank PLC"}
   {:id :post-office-money-savings,
    :common-name "Post Office Savings",
    :org-name "Post Office Money Savings"}
   {:id :rbs-bereavement-service-team,
    :common-name "Royal Bank of Scotland",
    :org-name "RBS Bereavement Service Team"}
   {:id :saga, :common-name "Saga", :org-name "Saga"}
   {:id :sainsburys-bank,
    :common-name "Sainsbury's Bank",
    :org-name "Sainsbury's Bank"}
   {:id :santander, :common-name "Santander", :org-name "Santander"}
   {:id :skipton-building-society,
    :common-name "Skipton Building Society",
    :org-name "Skipton Building Society"}
   {:id :tesco-bank, :common-name "Tesco Bank", :org-name "Tesco Bank"}
   {:id :the-cooperative-bank,
    :common-name "The Co-operative Bank",
    :org-name "The Co-operative Bank"}
   {:id :tsb-estate-settlement-unit,
    :common-name "TSB Bank",
    :org-name "TSB Estate Settlement Unit"}
   {:id :virgin-money,
    :common-name "Virgin Money",
    :org-name "Virgin Money"}
   {:id :yorkshire-building-society,
    :common-name "Yorkshire Building Society",
    :org-name "Yorkshire Building Society"}])

(def bank-by-id
  (into {} (map (juxt :id identity) bank-list)))

(defn all-bank-ids []
  (map :id bank-list))

(defn bank-label [bank-id]
  (get-in bank-by-id [bank-id :common-name]))
