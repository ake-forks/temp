(ns darbylaw.api.smart-search.data)


(def uk-aml--schema
  [:map
   [:client_ref {:optional true} :string]
   [:cra {:optional true}
    [:enum "experian" "equifax"]]
   [:risk_level [:enum "normal" "high"]]
   [:name [:map
           [:title [:string {:max 20}]]
           [:first [:string {:max 100}]]
           [:middle {:optional true} [:string {:max 100}]]
           [:last [:string {:max 100}]]]]
   [:date_of_birth {:optional true}
    [:re #"\d{4}-\d{2}-\d{2}"]]
   [:contacts {:optional true}
    [:map
     [:telephone {:optional true} [:string {:max 50}]]
     [:mobile {:optional true} [:string {:max 50}]]
     [:email {:optional true} [:string {:max 200}]]]]
   #_ ;; TODO
   (comment
     [:documents {:optional true}
      [:map
       [:nino {:optional true} [:string {:min 8 :max 9}]]]]
     [:bank {:optional true}
      [:map]])
   [:addresses
    [:vector {:min 1}
     [:map
      [:flat {:optional true} [:string {:max 100}]]
      [:building [:string {:max 100}]]
      [:street_1 [:string {:max 200}]]
      [:street_2 {:optional true} [:string {:max 200}]]
      [:town [:string {:max 50}]]
      [:region {:optional true} [:string {:max 50}]]
      [:postcode [:string {:max 12}]]
      [:duration {:optional true} [:int {:min 0}]]]]]])
