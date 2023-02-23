(ns darbylaw.api.smart-search.data
  (:require [darbylaw.api.util.malli :as malli+]))

(defn uk-aml->result
  [{:keys [result]}]
  (keyword result))

(defn fraudcheck->result
  [{:keys [result]}]
  (if (= result "low_risk")
    :pass
    :fail))

(defn smartdoc->status
  [{:keys [status]}]
  (if (contains? #{"processed" "failed" "invalid"} status)
    :processed
    :processing))

(defn smartdoc->result
  [{:keys [result] :as data}]
  (if (= :processed (smartdoc->status data))
    (keyword result)
    :processing))

(def uk-aml--schema
  [:map
   [:client_ref {:optional true} [:string {:max 30}]]
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

(def document-types--schema
  [:enum
   "driving_licence" "health_services_card" "id_card"
   "passport" "residence_permit" "visa"])

(def smartdoc--schema
  [:and
   [:map
    [:client_ref {:optional true} [:string {:max 30}]]
    [:sanction_region [:enum "gbr" "usa"]]
    [:name [:map
            [:title [:string {:max 20}]]
            [:first [:string {:max 100}]]
            [:middle {:optional true} [:string {:max 100}]]
            [:last [:string {:max 100}]]]]
    [:gender [:enum "male" "female"]]
    [:date_of_birth [:re #"\d{4}-\d{2}-\d{2}"]]
    [:address
     [:map
      [:flat {:optional true} [:string {:max 100}]]
      [:building [:string {:max 100}]]
      [:street_1 {:optional true} [:string {:max 100}]]
      [:street_2 {:optional true} [:string {:max 200}]]
      [:town {:optional true} [:string {:max 50}]]
      [:region {:optional true} [:string {:max 50}]]
      [:postcode [:string {:max 12}]]
      [:country [:string {:min 3 :max 3}]]]]
    [:issuing_country [:string {:min 3 :max 3}]]
    [:document_type
     [:or
      document-types--schema
      [:vector {:min 1} document-types--schema]]]
    [:scan_type
     [:enum
      "basic" "basic_selfie" "enhanced" "enhanced_selfie"]]
    ;; Required if `scan_type == basic_selfie`
    [:mobile_number {:optional true} [:string]]
    [:callback_url {:optional true} [:string]]
    [:is_journey_start_url_required {:optional true} [:boolean]]
    [:exit_url {:optional true} [:string]]
    ;; The docs say this is required, but I've not found this to be true
    [:document {:optional true}
     [:map
      [:front [:string]]
      [:back {:optional true} [:string]]]]]
   (malli+/match-then-required
     [:map
      [:scan_type [:enum "basic_selfie" "enhanced_selfie"]]]
     [:mobile_number])])

(def fraudcheck--schema
  [:map
   [:client_ref {:optional true} [:string {:max 30}]]
   [:sanction_region [:enum "gbr" "usa"]]
   [:name [:map
           [:first [:string {:max 100}]]
           [:last [:string {:max 100}]]]]
   [:date_of_birth [:re #"\d{4}-\d{2}-\d{2}"]]
   [:contacts
    [:map
     [:mobile :string]
     [:email {:optional true} :string]]]
   [:address
    [:and
     [:map
      [:line_1 :string]
      [:line_2 {:optional true} :string]
      [:city :string]
      [:state {:optional true} [:string {:min 2 :max 2}]]
      [:postcode :string]
      [:country [:string {:min 3 :max 3}]]]
     (malli+/match-then-required
       [:map
        [:country [:= "usa"]]]
       [:state])]]])
