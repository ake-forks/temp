(ns darbylaw.web.ui.other.form
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.ui.case-model :as case-model]
            [darbylaw.web.ui.other.model :as model]
            [darbylaw.web.util.form :as form]
            [darbylaw.web.ui :as ui :refer [<<]]
            [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [vlad.core :as v]
            [darbylaw.web.util.vlad :as v-util]))

(defn name-field [fork-args]
  [form/text-field fork-args
   {:name :name
    :label "name"
    :required true}])

(defn note [fork-args]
  [form/text-field fork-args
   {:name :note
    :label "note"
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

(defn value [fork-args]
  [currency-field fork-args
   {:name :value
    :label "value"
    :full-width true}])

(defn paid [{:keys [values handle-change]}]
  [mui/form-control-label
   {:label "paid?"
    :control
    (r/as-element
      [mui/checkbox
       {:name :paid
        :value (:paid values false)
        :checked (:paid values false)
        :on-change #(handle-change %)}])}])

(defn paid-at [fork-args]
  [form/date-picker fork-args
   {:name :paid-at
    :inner-config {:label "paid at"
                   :required true}}])

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

(defn no-doc-alert []
  [mui/alert {:severity :info}
   [mui/alert-title "No documents uploaded"]
   "If appropriate, we recommend uploading an invoice or receipt"])

(defn existing-documents [asset-id]
  (let [case-id (<< ::case-model/case-id)
        existing-files (:documents (<< ::model/asset asset-id))]
    [:<>
     [document-title
      {:title "Uploaded documents"
       :store-fn #(let [file (-> % .-target .-files first)]
                    (rf/dispatch [::model/upload-document case-id asset-id file]))}]
     (if (empty? existing-files)
       [no-doc-alert]
       (->> existing-files
            (map (fn [{:keys [document-id original-filename]}]
                   ^{:key document-id}
                   [document-item 
                    {:filename original-filename
                     :download-link (str "/api/case/" case-id "/other/" asset-id "/document/" document-id)
                     :delete-fn #(rf/dispatch [::model/delete-document case-id asset-id document-id])}]))
            (interpose [mui/divider])
            (into [mui/stack])))]))

(defn find-files [values]
  (->> values
       (filter (fn [[k _v]]
                 (str/starts-with? (name k) "-file-")))
       (into {})))

(defn form-documents [{:keys [values reset set-values]}]
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
         [no-doc-alert]
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
    [name-field fork-args]
    [value fork-args]
    (when (neg? (:value values))
      [:<>
       [paid fork-args]
       (when (:paid values)
         [paid-at fork-args])])
    [note fork-args]
    (let [asset-id (<< ::model/dialog-context)]
      (if (and (not (nil? asset-id))
               (not (= :add asset-id)))
        [existing-documents asset-id]
        [form-documents fork-args]))
    [submit-buttons fork-args]]])

(def data-validation
  (v/join
    (v/attr [:name] (v/present))
    (v-util/v-when #(:value %)
      (v/attr [:value] (v-util/currency?)))
    (v-util/v-when #(true? (:paid %))
      (v/attr [:paid-at] (v/chain
                           (v-util/not-nil)
                           (v-util/valid-dayjs-date))))))

(defn form [values on-submit]
  [form/form {:validation #(v/field-errors data-validation %)
              :on-submit on-submit
              :initial-values (or values {})}
   layout])
