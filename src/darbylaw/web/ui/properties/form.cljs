(ns darbylaw.web.ui.properties.form
  (:require
    [clojure.string :as str]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.components.file-input-button :refer [file-input-button]]
    [fork.re-frame :as fork]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.properties.model :as model]
    [reagent.core :as r]
    [clojure.edn :refer [read-string]]
    [darbylaw.web.ui :as ui :refer (<<)]))

(defn address-field [{:keys [values set-handle-change set-values] :as fork-args}]
  (let [non-owned (filter #(nil? (:owned? %)) (<< ::case-model/properties))
        deceased-address (<< ::case-model/deceased-address)]
    [mui/stack {:spacing 1}
     [mui/form-control {:full-width true
                        :variant :filled
                        :required true}
      [mui/input-label {:id :property} "property address"]
      [mui/select
       {:name :property
        :label-id :property
        :label "property address"
        :value (if-let [p (get values :property)]
                 (pr-str p)
                 "")
        :onChange (fn [evt]
                    (set-handle-change
                      {:value (read-string (ui/event-target-value evt))
                       :path [:property]}))}
       (interpose
         [mui/divider]
         (map (fn [property]
                ^{:key (:id property)}
                [mui/menu-item {:value (pr-str (:id property))}
                 [mui/typography {:white-space :pre} (str (:address property))]])
           non-owned))
       [mui/divider]
       [mui/menu-item {:value (pr-str :new-property)} "add new address"]]]
     (when (= (:property values) :new-property)
       [form-util/text-field fork-args
        {:name :address
         :label "address"
         :multiline true
         :minRows 3
         :fullWidth true
         :required true
         :InputProps
         (when (str/blank? (:address values))
           {:endAdornment
            (r/as-element
              [mui/button {:variant :text
                           :size :small
                           :style {:font-weight :normal}
                           :on-click #(set-values {:address deceased-address})
                           :start-icon (r/as-element [ui/icon-copy])}
               "copy from death certificate"])})}])]))

(defn joint-owner-field [{:keys [values handle-change] :as fork-args}]
  (let [joint? (or (:joint-ownership? values) false)]
    [mui/stack {:spacing 0.5}
     [mui/stack {:direction :row :spacing 0.5 :align-items :center}
      [mui/typography {:variant :body2
                       :color (if joint? theme/off-white :grey)} "no"]
      [mui/switch
       {:name :joint-ownership?
        :checked joint?
        :on-change handle-change}]
      [mui/typography {:variant :body2
                       :color (if joint? theme/teal :grey)} "yes, joint owned"]]
     (when joint?
       [form-util/text-field fork-args
        {:name :joint-owner
         :full-width true
         :label "name of other owner(s)"}])]))

(defn value-field [{:keys [values handle-change] :as fork-args}]
  [mui/stack {:direction :row :spacing 1 :align-items :center}
   [form-util/text-field fork-args
    {:name :valuation
     :label "value"
     :fullWidth true
     :InputProps
     {:start-adornment
      (r/as-element [mui/input-adornment
                     {:position :start} "£"])}}]
   [mui/stack {:spacing 0.5}
    [mui/typography {:variant :body2 :color (when (:estimated-value? values)
                                              theme/teal)} "estimate"]
    [mui/switch
     {:name :estimated-value?
      :label "estimate"
      :checked (or (:estimated-value? values) false)
      :on-change handle-change}]]])

(defn insured-field [{:keys [values handle-change]}]
  (let [insured? (or (:insured? values) false)]
    [mui/stack {:spacing 0.5}
     [mui/typography {:variant :body1} "Is the property insured?"]
     [mui/stack {:direction :row :spacing 0.5 :align-items :center}
      [mui/typography {:variant :body2
                       :color (if insured? theme/off-white :grey)} "no"]
      [mui/switch
       {:name :insured?
        :checked insured?
        :on-change handle-change}]
      [mui/typography {:variant :body2
                       :color (if insured? theme/teal :grey)} "yes, currently insured"]]]))

(defn get-files [values]
  (dissoc values model/non-file-fields))
(defn remove-document [file-idx {:keys [reset values]}]
  (let [new-vals (dissoc values file-idx)]
    (reset {:values new-vals})))

(defn documents-field [{:keys [set-values values] :as fork-args}]
   [mui/stack {:spacing 0.5
               :align-items :flex-start
               :style {:width "50%"}}
    (for [[idx file] (apply dissoc values model/non-file-fields)]
      ^{:key idx}
      [mui/stack {:direction :row
                  :justify-content :space-between
                  :align-items :center
                  :style {:width "100%"}}
       [mui/link {:variant :body1
                  :style {:text-decoration :none}} (.-name file)]
       [mui/icon-button {:on-click #(remove-document idx fork-args)}
        [ui/icon-close]]])
    [file-input-button
     {:button-props {:variant :outlined}
      :accept ".pdf, .jpeg, .jpg"
      :on-selected (fn [f]
                     (let [fileno (+ 1 (:file-count values))]
                       (set-values {(keyword (str "file-" fileno)) f
                                    :file-count fileno})))}
     "add document"]])

(defonce form-state (r/atom nil))
(defn form [{:keys [layout submit-fn initial-values]}]
  (r/with-let []
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit submit-fn
        :keywordize-keys true
        :prevent-default? true
        :initial-values initial-values}
       (fn [fork-args]
         [layout (ui/mui-fork-args fork-args)])]
    (finally
      (reset! form-state nil))))