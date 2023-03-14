(ns darbylaw.web.ui.bills.common
  (:require
    [clojure.string :as str]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [clojure.edn :refer [read-string]]
    [reagent.core :as r]))

(defn address-box [selected? child]
  [mui/paper (merge
               {:variant :outlined
                :sx (merge
                      {:flex-grow 1
                       :border-width 2
                       :padding 1
                       :white-space :pre}
                      (when selected?
                        {:border-color :primary.light}))})
   child])

(defn property-select [{:keys [values set-handle-change] :as _fork-args} dialog-type]
  (let [properties @(rf/subscribe [::model/current-properties])]
    [mui/form-control  {:full-width true
                        :variant :filled
                        :required true
                        :disabled (= dialog-type :edit)}
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
          properties))
      [mui/divider]
      [mui/menu-item {:value (pr-str :new-property)} "add new address"]]]))

(defn new-property-input [{:keys [values set-values] :as fork-args}]
  (let [add-property? (= :new-property (get values :property))
        deceased-address @(rf/subscribe [::case-model/deceased-address])]
    (if add-property?
      [mui/stack
       [form-util/text-field fork-args
        {:name :address-new
         :hiddenLabel true
         :placeholder "enter new address"
         :multiline true
         :minRows 3
         :maxRows 5
         :variant :outlined
         :fullWidth true
         :required true
         :InputProps
         (when (str/blank? (:address-new values))
           {:endAdornment
            (r/as-element
              [mui/button {:variant :text
                           :size :small
                           :style {:font-weight :normal}
                           :on-click #(set-values {:address-new deceased-address})
                           :start-icon (r/as-element [ui/icon-copy])}
               "copy from death certificate"])})}]])))


(defn upload-button [_asset-type _case-id _asset-id _props _label]
  (r/with-let [_ (reset! model/file-uploading? false)
               filename (r/atom "")]
    (fn [asset-type case-id asset-id props label]
      [ui/loading-button (merge props {:component "label"
                                       :loading @model/file-uploading?})
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::model/upload-file asset-type case-id asset-id selected-file])
                                (reset! filename "")
                                (reset! model/file-uploading? true))
                   :hidden true
                   :sx {:display :none}
                   :inputProps {:type :file
                                :accept ".pdf, .png, .jpeg, .jpg, .gif"}}]])))