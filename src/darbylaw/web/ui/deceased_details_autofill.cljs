(ns darbylaw.web.ui.deceased-details-autofill
  (:require [clojure.string :as str]
            [darbylaw.web.ui :as ui]
            [medley.core :as medley]
            [re-frame.core :as rf]
            [darbylaw.web.ui.util.fuzzy-match :as fuzzy-match]
            [darbylaw.web.ui.util.written-dates :as written-dates]))

(def header-search-strings
  {:entry-number "Entry No"
   :registration-district "Registration district"
   :date-and-place-of-death "1. Date and place of death"
   :name-and-surname "2. Name and surname"
   :sex "3. Sex"
   :maiden-name "4. Maiden surname of woman who has married"
   :date-and-place-of-birth "5. Date and place of birth"
   :name-of-informant "7.(a) Name and surname of informant"
   :name-of-registrar "11. Signature of registrar"})

(defn best-match [key-values search-string]
  (apply max-key :score
    (for [{:keys [key-words value-words]} key-values]
      (let [key-str (str/join " " key-words)]
        {:score (fuzzy-match/ratio search-string key-str)
         :key key-str
         :value-words value-words}))))

(defn find-relevant-kvs [ks]
  (into {}
    (for [[header search-string] header-search-strings]
      [header
       (let [match (best-match ks search-string)]
         (when (>= (:score match) 85)
           match))])))

(comment
  (fuzzy-match/ratio "Entry No" "Entry No.")
  (fuzzy-match/ratio "Entry No" "Entry N1.")
  (fuzzy-match/ratio "Entry Nooooooooooooooooo" "Entry Noooooooooooooooo1."))

(defn extract-certificate-number [lines]
  (->> lines
    (map :text)
    (keep #(re-find #"[A-Z]{3} [0-9]{6}" %))
    first))

(comment
  (extract-certificate-number [{:text "no match"}
                               {:text "  BAQ 901246"}])
  (extract-certificate-number [{:text "no match"}
                               {:text "  BAQ 90124X"}]))

(defn extract-date-and-place-of-death [value-words]
  (let [{:keys [date rest]} (written-dates/parse-date-leading (str/join " " value-words))]
    {:date-of-death date
     :place-of-death rest}))

(defn extract-name-and-surname [value-words]
  (let [all-uppercase? (fn [s]
                         (= s (str/upper-case s)))
        [forenames surnames] (->> value-words
                               (split-with #(not (and (all-uppercase? %)
                                                      (<= 1 (count %))))))]
    {:forename (str/join " " forenames)
     :surname (->> surnames
                (map str/lower-case)
                (map str/capitalize)
                (str/join " "))}))

(comment
  (extract-name-and-surname ["Doris", "SHERRINGTON"]))

(defn extract-date-and-place-of-birth [value-words]
  (let [{:keys [date rest]} (written-dates/parse-date-leading (str/join " " value-words))]
    {:date-of-birth date
     :place-of-birth rest}))

(defn strip-leading-number [s]
  (let [[_full-match text] (re-matches #"[0-9]?(.*)" s)]
    text))

(comment
  (strip-leading-number "8 and rest")
  (strip-leading-number "only-rest"))

(defn extract-cause-of-death [lines]
  (let [result-lines
        (->> lines
          (map #(-> % :text strip-leading-number))
          (drop-while #(let [ratio (fuzzy-match/ratio "Cause of death" %)]
                         (< ratio 85)))
          (drop 1)
          (take-while #(let [ratio (fuzzy-match/partial-ratio "Certified by" %)]
                         (< ratio 85))))]
    (when (<= (count result-lines) 6)
      (str/join "\n" result-lines))))

(comment
  (fuzzy-match/ratio "Cause of death" "8. Causx of death")
  (fuzzy-match/partial-ratio "Certified by" "Certified bx A.D. Odwell M.B."))

(defn extract-values [{:keys [key-values lines] :as response}]
  (let [relevant-kvs (find-relevant-kvs key-values)
        kv-words (fn [k]
                   (get-in relevant-kvs [k :value-words]))
        queries (->> response
                  :queries
                  (medley/index-by :query-alias))
        from-query (fn [query-alias confidence]
                     (let [q (get queries query-alias)]
                       (when (>= (:answer-confidence q) confidence)
                         (:answer-text q))))]
    (merge
      {:certificate-number (extract-certificate-number lines)
       :entry-number (first (kv-words :entry-number))
       :registration-district (str/join " " (kv-words :registration-district))}
      (extract-date-and-place-of-death (kv-words :date-and-place-of-death))
      (extract-name-and-surname (kv-words :name-and-surname))
      {:sex (some-> (first (kv-words :sex))
              str/lower-case
              #{"female" "male"})
       :maiden-name (->> (kv-words :maiden-name)
                      (map str/lower-case)
                      (map str/capitalize)
                      not-empty)}
      (extract-date-and-place-of-birth (kv-words :date-and-place-of-birth))
      {:occupation (from-query :occupation 50)
       :address (from-query :address 50)
       :name-of-informant (str/join " " (kv-words :name-of-informant))
       :name-of-doctor-certifying (from-query :certified-by 50)
       :cause-of-death (extract-cause-of-death lines)
       :name-of-registrar (->> (kv-words :name-of-registrar)
                            ; Don't remove the "registrar" title at the end
                            ;(remove #(= "registrar" (str/lower-case %)))
                            (str/join " "))})))

(comment
  #{"female" "male"} "female")

(rf/reg-fx ::change-fields
  (fn [{:keys [set-handle-change form-state-atom response]}]
    ; (def last-response response) ; for dev purposes
    (let [current-values (:values @form-state-atom)]
      (set-handle-change
        (keep identity
          (for [[field value] (extract-values response)]
            (when (and (some? value)
                       (or (nil? (get current-values field))
                           (str/blank? (get current-values field))))
              {:path [field]
               :value value})))))))

(rf/reg-event-fx ::autofill-success
  (fn [{:keys [db]} [_ set-handle-change form-state-atom response]]
    {:db (assoc db ::loading? false)
     ::change-fields {:set-handle-change set-handle-change
                      :form-state-atom form-state-atom
                      :response response}}))

(rf/reg-event-fx ::autofill-failure
  (fn [{:keys [db]} [_ result]]
    {:db (assoc db ::loading? false)
     ::ui/notify-user-http-error {:message "Could not extract text from document"
                                  :result result}}))

(rf/reg-event-fx ::autofill
  (fn [{:keys [db]} [_ case-id set-handle-change form-state-atom]]
    {:db (assoc db ::loading? true)
     :http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id "/document/death-certificate/analyze")
        :on-success [::autofill-success set-handle-change form-state-atom]
        :on-failure [::autofill-failure]})}))

(rf/reg-sub ::loading?
  (fn [db]
    (boolean (::loading? db))))