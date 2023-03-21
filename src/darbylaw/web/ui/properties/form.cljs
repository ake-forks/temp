(ns darbylaw.web.ui.properties.form
  (:require
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.components.file-input-button :refer [file-input-button]]
    [fork.re-frame :as fork]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.properties.model :as model]
    [reagent.core :as r]
    [darbylaw.web.ui :as ui :refer (<<)]))

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