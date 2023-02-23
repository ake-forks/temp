(ns darbylaw.web.ui.bills.common
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [clojure.edn :refer [read-string]]
    ))

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
  (let [deceased-address @(rf/subscribe [::case-model/deceased-address])
        properties @(rf/subscribe [::model/current-properties])]
    [mui/form-control {:full-width true
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

      [mui/menu-item {:value (pr-str :deceased)
                      :sx {:white-space :pre}}
       deceased-address]
      [mui/divider]
      (map (fn [property]
             ^{:key (:id property)}
             [mui/menu-item {:value (pr-str (:id property))}
              [mui/typography (str (:address property))]])
        properties)
      [mui/menu-item {:value (pr-str :new-property)} "add new address"]]]))

(defn new-property-input [{:keys [values] :as fork-args}]
  (let [add-property? (= :new-property (get values :property))]
    (if add-property?
      [form-util/text-field fork-args
       {:name :address-new
        :hiddenLabel true
        :placeholder "enter new address"
        :multiline true
        :minRows 3
        :maxRows 5
        :variant :outlined
        :fullWidth true
        :required true}])))