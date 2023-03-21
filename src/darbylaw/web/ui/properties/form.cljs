(ns darbylaw.web.ui.properties.form
  (:require
    [darbylaw.web.util.form :as form-util]
    [darbylaw.web.ui.components.file-input-button :refer [file-input-button]]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
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

(defn get-file-names [values]
  (select-keys values
    (filterv
      (fn [k] (re-find #"-filename-" (name k)))
      (keys values))))
(defn remove-document [file-name-key {:keys [set-values reset values]}]
  (let [fileno (-> file-name-key (name) (last) (js/parseInt))
        new-vals (dissoc values
                   (keyword (str "-file-" fileno))
                   file-name-key)]
    (reset {:values new-vals})))

(defn documents-field [{:keys [set-values values] :as fork-args}]
   [mui/stack {:spacing 0.5
               :align-items :flex-start
               :style {:width "50%"}}
    (for [[filename-key filename] (get-file-names values)]
      ^{:key filename-key}
      [mui/stack {:direction :row
                  :justify-content :space-between
                  :align-items :center
                  :style {:width "100%"}}
       [mui/link {:variant :body1
                  :style {:text-decoration :none}} filename]
       [mui/icon-button {:on-click #(remove-document filename-key fork-args)}
        [ui/icon-close]]])
    [file-input-button
     {:button-props {:variant :outlined}
      :accept ".pdf, .jpeg, .jpg"
      :on-selected (fn [f]
                     (let [fileno (+ 1 (:file-count values))]
                       (set-values {(keyword (str "file-" fileno)) f
                                    :file-count fileno})
                       (let [{:keys [tempfile content-type]} f]
                         (print tempfile)
                         (print content-type))))}
     "add document"]
    ;todo test button
    [mui/button {:on-click #(print values)} "print"]])

;(keyword (str "-filename-" fileno)) (.-name f)
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