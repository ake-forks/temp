(ns darbylaw.web.ui.funeral.util
  (:require
    [reagent.core :as r]
    [darbylaw.web.ui :as ui]))

(defn ->FormData
  [values]
  (let [form-data (js/FormData.)]
    (doseq [[k v] values]
      (.append form-data (name k) v))
    form-data))

(defn split-map
  [m ks]
  [(apply dissoc m ks)
   (select-keys m ks)])

(defn upload-button
  [{:keys [set-values values] :as fork-args}
   {:keys [name label inner-config] :as config}]
  (assert name "Missing required arg :name")
  (let [props {:component :label
               :variant :outlined
               :startIcon (r/as-element
                            (if-not (get values name)
                              [ui/icon-upload] 
                              [ui/icon-check]))} 
        prop-overrides (dissoc config :name :label :inner-config)
        
        inner-props
        {:type :file
         :hidden true
         :on-change #(set-values {name (-> % .-target .-files first)})}
        inner-prop-overrides inner-config]
    [ui/loading-button
     (merge props prop-overrides)
     (or label "Upload")
     [:input (merge inner-props inner-prop-overrides)]]))

(defn download-button
  [{:keys [label] :as config}]
  (let [props {:variant :outlined
               :download true
               :startIcon (r/as-element
                           [ui/icon-download])} 
        prop-overrides (dissoc config :name :label)]
    [ui/loading-button
     (merge props prop-overrides)
     (or label "Download")]))

