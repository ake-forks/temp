(ns darbylaw.web.ui.properties.form
  (:require
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.components.file-input-button :refer [file-input-button]]
    [fork.re-frame :as fork]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui :refer (<<)]))

(defonce form-state (r/atom nil))

(defn address-field [fork-args]
  [form-util/text-field fork-args
   {:name :address
    :label "address"
    :multiline true
    :minRows 3
    :fullWidth true
    :required true}])

(defn joint-owner-field [{:keys [values handle-change] :as fork-args}]
  [mui/stack {:direction :row
              :spacing 1
              :align-items :flex-start
              :sx {:height 1}}
   [mui/form-control-label
    {:control (r/as-element
                [mui/switch
                 {:name :joint-ownership?
                  :checked (or (:joint-ownership? values) false)
                  :label "estimated value"
                  :on-change handle-change}])
     :label-placement :end
     :label (r/as-element [mui/typography {:style {:white-space :pre}} "joint ownership"])}]
   (when (:joint-ownership? values)
    [form-util/text-field fork-args
     {:name :joint-owner
      :full-width true
      :label "name of other owner(s)"}])])

(defn value-field [fork-args]
  [form-util/text-field fork-args
   {:name :valuation
    :label "value"
    :fullWidth true
    :InputProps
    {:start-adornment
     (r/as-element [mui/input-adornment
                    {:position :start} "£"])}}])

(defn documents-field []
  [:<>
   [mui/stack {:spacing 0.5}
    [mui/link {:variant :body1
               :style {:text-decoration :none}} "zoopla_valuation.pdf"]]
   [file-input-button
    {:button-props {:variant :text}
     :accept ".pdf, .jpeg, .jpg"}
    "upload document"]])

(defn form [{:keys [layout submit-fn]}]
  (r/with-let []
      [fork/form
       {:state form-state
        :clean-on-unmount? true
        :on-submit submit-fn
        :keywordize-keys true
        :prevent-default? true}
       (fn [fork-args]
         [layout (ui/mui-fork-args fork-args)])]
    (finally
      (reset! form-state nil))))