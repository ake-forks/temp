(ns darbylaw.web.ui.vehicle.form
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.vehicle.model :as model]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.ui :as ui :refer [<<]]
            [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vlad.core :as v]
            [darbylaw.web.util.vlad :as v-util]))

(defn registration-number [fork-args]
  [form/text-field fork-args
   {:name :registration-number
    :label "registration number"
    :required true}])

(defn description [fork-args]
  [form/text-field fork-args
   {:name :description
    :label "description"
    :multiline true
    :maxRows 3
    :minRows 3}])

(defn currency-field [{:keys [values set-values] :as fork-args}
                      {:keys [name inner-config] :as config}]
  (assert name "Missing required arg :name")
  [mui/stack {:spacing 1 :direction :row}
   [form/text-field fork-args
    (merge (dissoc config :inner-config)
           {:InputProps
            {:start-adornment
             (r/as-element
               [mui/input-adornment {:position :start} "£"])}})]
   [mui/form-control-label
    (merge {:label "debt?"
            :checked (-> (get values name) (or "") (form/starts-with? "-"))
            :on-click #(set-values {name (-> (get values name) form/toggle-negative)})
            :control (r/as-element [mui/switch])}
           inner-config)]])

(defn estimated-value [fork-args]
  [currency-field fork-args
   {:name :estimated-value
    :label "estimated value"
    :full-width true}])

(defn sold [{:keys [values handle-change]}]
  [mui/form-control-label
   {:label "sold?"
    :control
    (r/as-element
      [mui/checkbox
       {:name :sold
        :value (:sold values false)
        :checked (:sold values false)
        :on-change #(handle-change %)}])}])

(defn sold-at [fork-args]
  [form/date-picker fork-args
   {:name :sold-at
    :inner-config {:label "sold at"
                   :required true}}])

(defn confirmed-value [fork-args]
  [currency-field fork-args
   {:name :confirmed-value
    :label "confirmed value"
    :full-width true
    :required true}])

(defn submit-buttons [_fork-args]
  [mui/stack {:spacing 1
              :direction :row}
   [mui/button {:variant :contained
                :full-width true
                :on-click #(rf/dispatch [::model/set-dialog-open])}
    "cancel"]
   [mui/button {:type :submit
                :variant :contained
                :full-width true
                :disabled (<< ::model/submitting?)}
    "save"]])

(defn document-item [{:keys [filename download-link delete-fn]}]
  [mui/stack {:spacing 1
              :direction :row
              :align-items :center}
   [mui/button {:variant :text
                :href download-link
                :target :_blank}
    filename]
   [mui/box {:flex-grow 1}]
   [mui/tooltip {:title "Delete"}
    [ui/icon-delete {:style {:cursor :pointer}
                     :on-click delete-fn}]]])

(defn document-title [{:keys [title store-fn]}]
  [mui/stack {:direction :row}
   [mui/typography {:variant :h6
                    :flex-grow 1}
    title]
   [mui/tooltip {:title "Upload new document"}
    [ui/loading-button {:component :label
                        :startIcon (r/as-element
                                     [ui/icon-upload])}
     "upload"
     [:input {:type :file
              :hidden true
              :on-change store-fn}]]]])

(defn no-doc-alert [{:keys [errors values attempted-submissions]}]
  (let [sold (:sold values)]
    [mui/alert {:severity (if sold
                            (if (and (get errors [:file-errors])
                                     (>= attempted-submissions 1))
                              :error
                              :warning)
                            :info)}
     [mui/alert-title "No documents selected"]
     (when sold "Minimum of one required")]))

(defn existing-documents [vehicle-id fork-args]
  (let [case-id (<< ::case-model/case-id)
        existing-files (:documents (<< ::model/vehicle vehicle-id))]
    [:<>
     [document-title
      {:title "Uploaded documents"
       :store-fn #(let [file (-> % .-target .-files first)]
                    (rf/dispatch [::model/upload-document case-id vehicle-id file]))}]
     (if (empty? existing-files)
       [no-doc-alert fork-args]
       (->> existing-files
            (map (fn [{:keys [document-id original-filename]}]
                   ^{:key document-id}
                   [document-item 
                    {:filename original-filename
                     :download-link (str "/api/case/" case-id "/vehicle/" vehicle-id "/document/" document-id)
                     :delete-fn #(rf/dispatch [::model/delete-document case-id vehicle-id document-id])}]))
            (interpose [mui/divider])
            (into [mui/stack])))]))

(defn find-files [values]
  (->> values
       (filter (fn [[k _v]]
                 (str/starts-with? (name k) "-file-")))
       (into {})))

(defn form-documents [{:keys [values reset set-values] :as fork-args}]
  (r/with-let [file-count (r/atom 0)]
    [:<>
     [document-title
      {:title "Upload documents"
       :store-fn #(let [file (-> % .-target .-files first)
                        file-id (swap! file-count inc)
                        file-key (keyword (str "-file-" file-id))]
                    (set-values {file-key file}))}]
     (let [files (find-files values)]
       (if (empty? files)
         [no-doc-alert fork-args]
         (->> files
              (map (fn [[key file]]
                     ^{:key key}
                     [document-item 
                      {:filename (.-name file)
                       :download-link (js/URL.createObjectURL file)
                       :delete-fn #(let [new-values (dissoc values key)]
                                     (reset {:values new-values}))}]))
              (interpose [mui/divider])
              (into [mui/stack]))))]))

(defn layout [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit
          :style {:width "100%"}}
   [mui/stack {:spacing 1}
    [registration-number fork-args]
    [description fork-args]
    [sold fork-args]
    (if-not (:sold values)
      [estimated-value fork-args]
      [:<>
       [sold-at fork-args]
       [confirmed-value fork-args]])
    (let [vehicle-id (<< ::model/dialog-context)]
      (if (and (not (nil? vehicle-id))
               (not (= :add vehicle-id)))
        [existing-documents vehicle-id fork-args]
        [form-documents fork-args]))
    [submit-buttons fork-args]]])

(defn min-one-files? [{:keys [fake-selector]}]
  (v/predicate (fn [data]
                 (let [context (<< ::model/dialog-context)
                       files (if (and (not= :add context)
                                      (not (nil? context)))
                               (:documents (<< ::model/vehicle context))
                               (vals (find-files data)))]
                   (< (count files) 1)))
               {:type ::min-one-files?
                :selector fake-selector}))

(defmethod v/english-translation ::min-one-files?
  [_]
  (str "Must have more than one files."))

(def data-validation
  (v/join
    (v/attr [:registration-number] (v/present))
    (v-util/v-when #(:estimated-value %)
      (v/attr [:estimated-value] (v-util/currency?)))
    (v-util/v-when #(true? (:sold %))
      (v/join
        (min-one-files? {:fake-selector [:file-errors]})
        (v/attr [:sold-at] (v/chain
                             (v-util/not-nil)
                             (v-util/valid-dayjs-date)))
        (v/attr [:confirmed-value] (v/chain
                                     (v/present)
                                     (v-util/currency?)))))))

(defn form [values on-submit]
  [form/form {:validation #(v/field-errors data-validation %)
              :on-submit on-submit
              :initial-values (or values {})}
   layout])
