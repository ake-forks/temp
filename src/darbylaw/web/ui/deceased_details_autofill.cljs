(ns darbylaw.web.ui.deceased-details-autofill
  (:require [clojure.string :as str]
            [darbylaw.web.ui :as ui]
            [re-frame.core :as rf]))

(defn aprox= [x y]
  (let [strip-punctuation (fn [s]
                            (str/replace s #"[^a-zA-Z0-9]" ""))]
    (cond
      (and (string? x)
           (string? y))
      (= (strip-punctuation x)
         (strip-punctuation y))

      (and (sequential? x)
           (sequential? y))
      (= (map strip-punctuation x)
         (map strip-punctuation y))

      :else
      (throw (ex-info "Unexpected arguments" {:x x :y y})))))

(comment
  (aprox= ["Entry" "No"] ["Entry" "No."]))

(defn extract-certificate-number [key-values]
  (->> key-values
    (keep (fn [{:keys [key-words value-words]}]
            (when (and (= 1 (count key-words))
                       (re-matches #"[A-Z]{3}" (first key-words)))
              (str (first key-words) " " (first value-words)))))
    first))

(defn extract-entry-number [key-values]
  (->> key-values
    (keep (fn [{:keys [key-words value-words]}]
            (when (aprox= ["Entry" "No"] key-words)
              (first value-words))))
    first))

(rf/reg-fx ::change-fields
  (fn [{:keys [set-handle-change response]}]
    (let [{:keys [key-values]} response]
      (set-handle-change [{:value (extract-certificate-number key-values)
                           :path [:certificate-number]}
                          {:value (extract-entry-number key-values)
                           :path [:entry-number]}]))))

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
