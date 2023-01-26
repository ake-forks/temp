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

(def bill-schema
  [:map
   [:company [:or
              (into [:enum] (map :id companies))
              [:string {:min 1}]]]
   [:bill-type [:set (into [:enum] (keys bill-types))]]])

(comment
  (require '[malli.core :as malli])
  (malli/explain bill-schema {:company :mine
                              :bill-type #{:ok}}))

(def bill-props
  (->> (rest bill-schema)
    (mapv first)))
