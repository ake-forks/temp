(ns darbylaw.web.ui.buildingsociety.shared
  (:require
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.buildingsociety.model :as model]
    [darbylaw.web.ui.buildingsociety.form :as form]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]))


(defn close-button-row []
  [mui/stack {:direction :row
              :style {:justify-content :end}}
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(defn close-button []
  [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])
                    :style {:align-self "flex-start"}}
   [ui/icon-close]])
(defn stepper [stage-keyword]
  (let [stage (case stage-keyword
                :edit 0
                :notify 1
                :valuation 2
                :complete 3)]
    [mui/stepper {:alternative-label false :active-step stage}
     [mui/step
      [mui/step-label "ADD ACCOUNTS"]]
     [mui/step
      [mui/step-label "SEND NOTIFICATION"]]
     [mui/step
      [mui/step-label "CONFIRM VALUES"]]]))

(defn title-only [text]
  [mui/stack {:direction :row :justify-content :space-between}
   [mui/typography {:variant :h4 :sx {:mb 1}} text]
   [close-button]])


(defn header [buildsoc-id stage-keyword]
  [mui/stack {:spacing 2}
   [mui/stack {:direction :row :justify-content :space-between}
    [mui/typography {:variant :h4 :sx {:mb 1}} (model/buildsoc-label buildsoc-id)]
    [close-button]]
   [stepper stage-keyword]])

(defn accounts-view [accounts {:keys [estimated? confirmed?]}]
  [mui/stack {:spacing 1}
   [mui/typography {:variant :h6} "account information"]
   (if (empty? accounts)
     [mui/typography {:variant :body1} "Account details unknown."]
     (map
       (fn [account]
         [mui/stack {:spacing 1 :direction :row}
          [mui/text-field {:name :roll-number
                           :value (get account :roll-number)
                           :label "roll number"
                           :disabled true
                           :full-width true}]
          (if estimated?
            [mui/text-field {:name :estimated-value
                             :value (get account :estimated-value)
                             :label "estimated value"
                             :disabled true
                             :full-width true
                             :InputProps
                             {:start-adornment
                              (r/as-element [mui/input-adornment
                                             {:position :start} "£"])}}])
          (if confirmed?
            [mui/text-field {:name :confirmed-value
                             :value (get account :confirmed-value)
                             :label "confirmed value"
                             :disabled true
                             :full-width true
                             :InputProps
                             {:start-adornment
                              (r/as-element [mui/input-adornment
                                             {:position :start} "£"])}}])])
       accounts))])

(defn upload-button [_case-id _buildsoc-id _props _label suffix]
  (r/with-let [_ (reset! model/file-uploading? false)
               filename (r/atom "")]
    (fn [case-id buildsoc-id props label]
      [ui/loading-button (merge props {:component "label"
                                       :loading @model/file-uploading?})
       label
       [mui/input {:type :file
                   :value @filename
                   :onChange #(let [selected-file (-> % .-target .-files first)]
                                (rf/dispatch [::model/upload-file case-id buildsoc-id selected-file suffix])
                                (reset! filename "")
                                (reset! model/file-uploading? true))
                   :hidden true
                   :sx {:display :none}}]])))

(defn submit-buttons [labels]
  [form/submit-buttons labels])

(def narrow-dialog-props
  {:style {:height "40vh"}
   :width "60vw"
   :padding "1rem"})

(def tall-dialog-props
  {:style {:height "90vh"}
   :width "70vw"
   :padding "1rem"})



