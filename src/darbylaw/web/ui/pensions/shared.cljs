(ns darbylaw.web.ui.pensions.shared
  (:require
    [darbylaw.web.util.form :as form-util]
    [fork.re-frame :as fork]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
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

(defn my-form
  [{:keys [values handle-change handle-blur]}]
  [:div
   [:p "Read back: " (values "input")]
   [:input
    {:name "input"
     :value (values "input")
     :on-change handle-change
     :on-blur handle-blur}]])

(defn foo []
  [fork/form {:initial-values
              {"input" "hello"}}
   my-form])

;
;{:provider :aviva, :ni-number "1", :reference "99", :pension-type :private, :id #uuid "21aef85b-240c-4bf1-8f61-9cc6cbdd96e3"}
(defn account-info [provider]
  (let [{:keys [reference id pension-type] :as data} (model/get-pension provider)
        case-id (<< ::case-model/case-id)]
    [fork/form {:on-submit
                #(rf/dispatch [::model/edit-pension
                               pension-type case-id id %])
                :prevent-default? true
                :clean-on-unmount? true
                :keywordize-keys true
                :initial-values
                {:reference reference :provider provider}}
     (fn [{:keys [handle-submit] :as fork-args}]
       [:form {:on-submit handle-submit}
        [mui/card
         [mui/card-content
          [mui/stack {:direction :row :justify-content :space-between :align-items :center}
           [mui/stack {:spacing 1}
            [mui/typography {:variant :body1 :style {:font-weight :bold}} (:ni-number data)]
            (if @model/edit-mode
              [form-util/text-field fork-args
               {:name :reference
                :label :reference}]
              [mui/typography {:variant :body1} (str "policy reference: " reference)])]
           (if @model/edit-mode
             [mui/button {:variant :outlined :type :submit} "save"]
             [mui/stack {:direction :row :spacing 2 :align-items :center}
              [mui/tooltip {:title "edit"}
               [mui/icon-button {:on-click #(reset! model/edit-mode true)
                                 :variant :outlined}
                [ui/icon-edit]]]
              [mui/tooltip {:title "remove"}
               [mui/icon-button {:on-click #(print "remove")
                                 :variant :outlined}
                [ui/icon-delete]]]])]]]])]))
