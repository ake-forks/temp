(ns darbylaw.web.ui.pensions.shared
  (:require
    [darbylaw.web.util.form :as form-util]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui :as ui :refer (<<)]
    [darbylaw.web.ui.pensions.model :as model]))

(defn dialog-header [title]
  [mui/stack {:direction :row
              :justify-content :space-between
              :sx {:mb 1}}
   [mui/typography {:variant :h5} title]
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(def dialog-size {:height "40vh" :width "40vw"})

;{:provider :aviva, :ni-number "1", :reference "99", :pension-type :private, :id #uuid "21aef85b-240c-4bf1-8f61-9cc6cbdd96e3"}
(defn account-info [provider]
  (let [data (model/current-pension provider)]
    [mui/card
     [mui/card-content
      [mui/stack {:direction :row :justify-content :space-between :align-items :center}
       [mui/stack {:spacing 1}
        [mui/typography {:variant :body1 :style {:font-weight :bold}} (:ni-number data)]
        (if @model/edit-mode
          [form-util/text-field {:values {:reference (:reference data)}} {:name :reference :label "reference"}]
          [mui/typography {:variant :body1} (str "policy reference: " (:reference data))])]

       (if @model/edit-mode
         [mui/button {:variant :outlined :on-click #(reset! model/edit-mode false)} "save"]
         [mui/stack {:direction :row :spacing 2 :align-items :center}
          [mui/tooltip {:title "edit"}
           [mui/icon-button {:on-click #(reset! model/edit-mode true)
                             :variant :outlined}
            [ui/icon-edit]]]
          [mui/tooltip {:title "remove"}
           [mui/icon-button {:on-click #(print "remove")
                             :variant :outlined}
            [ui/icon-delete]]]])]]]))
