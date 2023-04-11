(ns darbylaw.web.ui.other.dialog
  (:require
    [reagent-mui.components :as mui]
    [re-frame.core :as rf]
    [darbylaw.web.ui :as ui :refer [<<]]
    [darbylaw.web.ui.other.model :as model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.other.form :as form]))

(defn dialog-content []
  [mui/dialog-content
   [mui/stack {:spacing 3
               :direction :row}
    (let [case-id (<< ::case-model/case-id)
          context (<< ::model/dialog-context)]
      [form/form
       (if (= :add context) {} (<< ::model/asset-form-details context))
       #(let [context (<< ::model/dialog-context)]
          ;; NOTE: Pulling in dialog-context inside the function is required.
          ;;       If we pull it outside then it won't be updated and pressing
          ;;       save twice will add two assets.
          (rf/dispatch [::model/upsert-asset
                        (merge {:case-id case-id}
                               (when-not (= :add context)
                                 {:asset-id context}))
                        %]))])]])


(defn dialog []
  [mui/dialog {:open (boolean (<< ::model/dialog-open?))
               :max-width :sm
               :full-width true}
   [mui/backdrop {:open (boolean (<< ::model/submitting?))}
    [mui/circular-progress]]
   [mui/stack {:spacing 1}
    [mui/dialog-title
     [mui/stack {:spacing 1 :direction :row}
      [mui/typography {:variant :h4}
       (if (= :add (<< ::model/dialog-context))
         "add other"
         "edit other")]
      [mui/box {:flex-grow 1}]
      [ui/icon-close {:style {:cursor :pointer}
                      :on-click #(rf/dispatch [::model/set-dialog-open])}]]]
    [dialog-content]]])
