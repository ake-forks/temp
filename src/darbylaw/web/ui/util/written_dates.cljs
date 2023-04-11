(ns darbylaw.web.ui.util.written-dates
  (:require [clojure.string :as str]
            ["chrono-node" :as chrono]
            [darbylaw.web.util.dayjs :as dayjs]))

(defn parse-date-leading [s]
  (when-let [result-js (first (chrono/parse s))]
    (let [result (-> result-js
                   js/JSON.stringify js/JSON.parse
                   (js->clj :keywordize-keys true))
          date-fields (some-> result :start :knownValues
                        ((juxt :year :month :day)))]
      (when (and (some? date-fields)
                 (every? integer? date-fields))
        {:date (dayjs/read (str/join "-" date-fields))
         :rest (str/trim (subs s (+ (:index result)
                                    (count (:text result)))))}))))

(comment
  (parse-date-leading "Fifth January 2008 Duncote Hall Nursing Home, Duncote, Towcester")
  (parse-date-leading "Duncote Hall Nursing Home, Duncote, Towcester"))