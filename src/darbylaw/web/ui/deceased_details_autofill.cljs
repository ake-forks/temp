(ns darbylaw.web.ui.deceased-details-autofill
  (:require [clojure.string :as str]
            [darbylaw.web.ui :as ui]
            [medley.core :as medley]
            [re-frame.core :as rf]
            ["chrono-node" :as chrono]
            [darbylaw.web.util.dayjs :as dayjs]
            ["fuzzball" :as fuzz]))

(def header-search-strings
  {:entry-number "Entry No"
   :registration-district "Registration district"
   :date-and-place-of-death "1. Date and place of death"
   :name-and-surname "2. Name and surname"
   :sex "3. Sex"
   :maiden-name "4. Maiden surname of woman who has married"
   :date-and-place-of-birth "5. Date and place of birth"
   :name-of-informant "7.(a) Name and surname of informant"
   :cause-of-death "8. Cause of death"
   :name-of-registrar "11. Signature of registrar"})

(defn best-match [key-values search-string]
  (apply max-key :score
    (for [{:keys [key-words value-words]} key-values]
      (let [key-str (str/join " " key-words)]
        {:score (.ratio fuzz search-string key-str)
         :key key-str
         :value-words value-words}))))

(defn find-headers [key-values]
  (into {}
    (for [[header search-string] header-search-strings]
      [header
       (let [match (best-match key-values search-string)]
         (when (>= (:score match) 85)
           match))])))

(comment
  (.ratio fuzz "Entry No" "Entry No.")
  (.ratio fuzz "Entry No" "Entry N1.")
  (.ratio fuzz "Entry Nooooooooooooooooo" "Entry Noooooooooooooooo1."))

(defn extract-certificate-number [key-values]
  (->> key-values
    (keep (fn [{:keys [key-words value-words]}]
            (when (and (= 1 (count key-words))
                       (re-matches #"[A-Z]{3}" (first key-words)))
              (str (first key-words) " " (first value-words)))))
    first))

(defn chrono-parse [free-text]
  (when-let [result-js (first (chrono/parse free-text))]
    (let [result (-> result-js
                   js/JSON.stringify js/JSON.parse
                   (js->clj :keywordize-keys true))
          date-fields (some-> result :start :knownValues
                        ((juxt :year :month :day)))]
      (when (and (some? date-fields)
                 (every? integer? date-fields))
        {:date (dayjs/read (str/join "-" date-fields))
         :rest (str/trim (subs free-text (+ (:index result)
                                           (count (:text result)))))}))))

(comment
  (chrono-parse "Fifth January 2008 Duncote Hall Nursing Home, Duncote, Towcester"))

(defn extract-date-and-place-of-death [value-words]
  (let [{:keys [date rest]} (chrono-parse (str/join " " value-words))]
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
  (let [{:keys [date rest]} (chrono-parse (str/join " " value-words))]
    {:date-of-birth date
     :place-of-birth rest}))

(defn extract-values [{:keys [key-values] :as response}]
  (let [known-header-values (find-headers key-values)
        words (fn [header]
                (get-in known-header-values [header :value-words]))
        queries (->> response
                  :queries
                  (medley/index-by :query-alias))
        from-query (fn [query-alias confidence]
                     (let [q (get queries query-alias)]
                       (when (>= (:answer-confidence q) confidence)
                         (:answer-text q))))]
    (merge
      {:certificate-number (extract-certificate-number key-values)
       :entry-number (first (words :entry-number))
       :registration-district (str/join " " (words :registration-district))}
      (extract-date-and-place-of-death (words :date-and-place-of-death))
      (extract-name-and-surname (words :name-and-surname))
      {:sex (some-> (first (words :sex))
              str/lower-case
              #{"female" "male"})
       :maiden-name (->> (words :maiden-name)
                      (map str/lower-case)
                      (map str/capitalize)
                      not-empty)}
      (extract-date-and-place-of-birth (words :date-and-place-of-birth))
      {:occupation (from-query :occupation 50)
       :address (from-query :address 50)
       :name-of-informant (str/join " " (words :name-of-informant))
       :name-of-doctor-certifying (from-query :certified-by 50)
       :cause-of-death (str/join " " (words :cause-of-death))
       :name-of-registrar (->> (words :name-of-registrar)
                            (remove #(= "registrar" (str/lower-case %)))
                            (str/join " "))})))

(comment
  #{"female" "male"} "female")

(rf/reg-fx ::change-fields
  (fn [{:keys [set-handle-change response]}]
    (def last-response response)
    (set-handle-change
      (keep identity
        (for [[field value] (extract-values response)]
          {:path [field]
           :value value})))))

(rf/reg-event-fx ::autofill-success
  (fn [_ [_ set-handle-change response]]
    {::change-fields {:set-handle-change set-handle-change
                      :response response}}))

(rf/reg-event-fx ::autofill-failure
  (fn [_ [_ result]]
    {::ui/notify-user-http-error {:message "Could not extract text from document"
                                  :result result}}))

(rf/reg-event-fx ::autofill
  (fn [_ [_ case-id set-handle-change]]
    {:http-xhrio
     (ui/build-http
       {:method :get
        :uri (str "/api/case/" case-id "/document/death-certificate/analyze")
        :on-success [::autofill-success set-handle-change]
        :on-failure [::autofill-failure]})}))
