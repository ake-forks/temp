(ns darbylaw.web.ui.buildingsociety.form
  (:require [reagent-mui.components :as mui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [darbylaw.web.ui :as ui]
            [darbylaw.web.ui.buildingsociety.model :as model])
  (:require-macros [reagent-mui.util :refer [react-component]]))

(def buildsoc-options
  [{:id :bath-building-society
    :common-name "Bath Building Society"}
   {:id :cambridge-building-society
    :common-name "Cambridge Building Society"}
   {:id :darlington-building-society
    :common-name "Darlington Building Society"}
   {:id :harpenden-building-society
    :common-name "Harpenden Building Society"}])

(defn buildsoc-select [{:keys [values set-handle-change handle-blur touched errors] :as fork-args}]
  [mui/autocomplete
   {:options (map :id buildsoc-options)
    :value (get values :buildsoc-id)
    :onChange (fn [_evt new-value]
                (set-handle-change {:value new-value
                                    :path [:buildsoc-id]}))
    :renderInput (react-component [props]
                   [mui/text-field (merge props
                                     {:name :buildsoc-id
                                      :label "building society name"
                                      :required true
                                      :onBlur handle-blur})])}])


;accounts can
(defn account-array
  [props
   {:fieldarray/keys [fields
                      insert
                      remove
                      handle-change
                      handle-blur]}]
  [mui/stack {:spacing 1}
   (doall
     (->> fields
       (map-indexed
         (fn [idx field]
           ^{:key idx}
           [mui/box
            [mui/stack {:spacing 1 :direction :row}
             [mui/text-field {:name :roll-number
                              :value (get field :roll-number)
                              :label "roll number"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true}]
             [mui/text-field {:name :estimated-value
                              :value (get field :estimated-value)
                              :label "estimated value"
                              :on-change #(handle-change % idx)
                              :on-blur #(handle-blur % idx)
                              :required true
                              :full-width true
                              :InputProps
                              {:start-adornment
                               (r/as-element [mui/input-adornment
                                              {:position :start} "£"])}}]
             [mui/icon-button {:on-click #(remove idx)}
              [ui/icon-delete]]]]))))
   [mui/button {:on-click #(insert {:roll-number "" :estimated-value ""})
                :style {:text-transform "none" :align-self "baseline" :font-size "1rem"}
                :variant :text
                :size "large"
                :full-width false
                :start-icon (r/as-element [ui/icon-add-circle])}
    (str "add another "
      (if-let [buildsoc-id (get-in props [:values :buildsoc-id])]
        (str buildsoc-id " account")
        "account"))]])

(defn account-array-component [fork-args]
  [fork/field-array {:props fork-args
                     :name :accounts}
   account-array])

(defn accounts-unknown [{:keys [values handle-change handle-blur touched errors] :as fork-args}]
  [mui/form-group
   [mui/form-control-label {
                            :control (r/as-element
                                       [mui/checkbox {:name :accounts-unknown
                                                      :value (:accounts-unknown values)
                                                      :checked (:accounts-unknown values)
                                                      :label "accounts not known"
                                                      :onChange handle-change}])
                            :label "account details not known"}]])

(defn submit-buttons []
  [mui/stack {:spacing 1
              :direction :row
              :justify-content :space-between}
   [mui/button {:onClick #(rf/dispatch [::model/hide-dialog])
                :variant :contained :full-width true} "cancel"]
   [mui/button {:type :submit :variant :contained :full-width true} "save"]])


(defn form-component [{:keys [values handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/stack {:spacing 1}
    [buildsoc-select fork-args]
    [mui/typography {:variant :body1}]]])




(defonce form-state (r/atom nil))

(defn form [form-component initial-values]
  (r/with-let []
    (let [case-id (-> @(rf/subscribe [::ui/path-params]) :case-id)]
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit #(print %)
        :keywordize-keys true
        :prevent-default? true
        :initial-values initial-values}                     ; placeholder for entering first account
       (fn [fork-args]
         [form-component (ui/mui-fork-args fork-args)])])
    (finally
      (reset! form-state nil))))
