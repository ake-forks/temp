(ns darbylaw.web.ui.properties.dialog
  (:require
    [darbylaw.api.util.data :as data-util]
    [darbylaw.web.theme :as theme]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.properties.model :as model]
    [darbylaw.web.ui.properties.form :as form]
    [darbylaw.web.ui :as ui :refer (<<)]
    [reagent.core :as r]))

(defn dialog-header [title]
  [mui/stack {:direction :row
              :justify-content :space-between}
   [mui/typography {:variant :h5} title]
   [mui/icon-button {:on-click #(rf/dispatch [::model/hide-dialog])}
    [ui/icon-close]]])

(defn documents-panel [{:keys [documents id]}]
  (let [case-id (<< ::case-model/case-id)
        property-id (:id (<< ::model/dialog))
        edit-mode @model/edit-mode]
    [mui/paper {:sx {:p 2}}
      [mui/stack {:spacing 1}
       [mui/typography {:variant :h6
                        :style {:color (if edit-mode :grey)}} "valuation documents"]

       [mui/card-content {:style {:height "10rem" :overflow-y :auto :overflow-x :auto :padding 0}}
        (if (some? documents)
          (map
            (fn [document]
              ^{:key (:filename document)}
              [mui/stack {:direction :row
                          :justify-content :space-between
                          :align-items :center}
               [mui/link {:variant :body1
                          :style {:text-decoration :none
                                  :max-width "75%"
                                  :overflow :wrap
                                  :color (if edit-mode :grey)}} (:original-filename document)]
               [mui/stack {:direction :row :spacing 2}
                [mui/tooltip {:title "open"}
                 [mui/icon-button {:on-click #(rf/dispatch [::model/open-document case-id (:filename document)])
                                   :disabled edit-mode}
                  [ui/icon-open-in-new]]]
                [mui/tooltip {:title "remove"}
                 [mui/icon-button {:on-click #(rf/dispatch [::model/remove-file case-id property-id (:filename document)])
                                   :disabled edit-mode}
                  [ui/icon-delete]]]]])
            documents)
          [mui/typography {:variant :body1} "no documents uploaded"])]
       (if edit-mode
         [mui/button {:disabled true
                      :end-icon (r/as-element [ui/icon-upload])} "upload"]
         [form/upload-button case-id id "upload" {:disabled edit-mode}])]]))

(defn edit [{:keys [handle-submit] :as fork-args}]
  (let [prop-id (:id (<< ::model/dialog))
        property (model/get-property prop-id)
        edit-mode model/edit-mode]
    [:form {:on-submit handle-submit}
     [mui/dialog-content {:style {:width "50vw"}}
      [mui/stack {:spacing 1}
       [dialog-header (data-util/first-line (:address property))]
       (if @edit-mode
         [mui/button {:on-click #(reset! edit-mode (not @edit-mode))
                      :end-icon (r/as-element [ui/icon-close])
                      :style {:align-self :flex-start}} "cancel"]
         [mui/button {:variant :outlined
                      :on-click #(reset! edit-mode (not @edit-mode))
                      :end-icon (r/as-element [ui/icon-edit])
                      :style {:align-self :flex-start}} "edit property details"])
       [mui/stack {:spacing 2}
        [mui/grid {:container true
                   :direction :row
                   :spacing 1
                   :align-items :stretch}
         [mui/grid {:item true :xs 6}
          [mui/typography {:variant :h6} "address"]
          (if @edit-mode
            [form/edit-address fork-args]
            [mui/typography {:variant :body1  :sx {:mt 1} :style {:white-space :pre}} (:address property)])
          [mui/typography {:variant :h6 :sx {:mt 2}} "valuation"]
          (if @edit-mode
            [form/value-field fork-args]
            [:<>
             [mui/typography {:variant :body1 :style {:white-space :pre}} "£" (:valuation property)]
             (when (:estimated-value? property)
               [mui/typography {:variant :body1 :color theme/teal} "estimated"])])]


         [mui/grid {:item true :xs 6 :style {:max-height "50%"}}
          [documents-panel property]]
         [mui/grid {:item true :xs 6}
          (if @edit-mode
            [:<>
             [mui/typography {:variant :h6} "joint ownership"]
             [form/joint-owner-field fork-args]]
            (when (:joint-ownership? property)
              [:<>
               [mui/typography {:variant :h6} "joint ownership"]
               [mui/typography {:variant :body1 :style {:white-space :pre}} (or (:joint-owner property) "please provide details for the other owner(s)")]]))]
         [mui/grid {:item true :xs 6}
          [mui/typography {:variant :h6} "insurance details"]
          (if @edit-mode
            [form/insured-field fork-args]
            (if (:insured? property)
              [mui/stack {:direction :row :spacing 2}
               [ui/icon-check-base {:style {:color theme/teal}}]
               [mui/typography {:variant :body1 :color theme/teal} "property is insured"]]
              [mui/stack {:direction :row :spacing 2}
               [ui/icon-close {:style {:color theme/orange}}]
               [mui/typography {:color theme/orange} "property is not insured"]]))]]]]]


     [mui/dialog-actions
      (if @edit-mode
        [mui/stack {:direction :row :spacing 2}
         [mui/button {:on-click #(reset! edit-mode (not @edit-mode)) :variant :outline} "cancel"]
         [mui/button {:type :submit :variant :contained} "save"]]
        [mui/button {:variant :outlined
                     :on-click #(rf/dispatch [::model/hide-dialog])} "close"])]]))

(defn add [{:keys [handle-submit] :as fork-args}]
  [:form {:on-submit handle-submit}
   [mui/dialog-content {:style {:width "40vw" :height "60vh"}}
    [dialog-header "add property"]
    [mui/stack {:spacing 1}
     [mui/typography {:variant :h6} "details"]
     [form/address-field fork-args]
     [form/joint-owner-field fork-args]
     [form/insured-field fork-args]
     [mui/typography {:variant :h6} "valuation"]
     [mui/typography {:variant :body1}
      "You can use "
      [mui/link {:href "https://www.zoopla.co.uk/home-values/"
                 :underline :hover
                 :target :blank
                 :rel "noopener noreferrer"} "this third-party tool "
       [ui/icon-open-in-new]]
      " to obtain an estimated value of the property and enter it as an 'estimate' below.
      If you have received a professional valuation, please upload supporting documentation."]
     [form/value-field fork-args]
     [mui/typography {:variant :h6} "supporting documents"]
     [form/documents-field fork-args]]]
   [mui/dialog-actions
    [mui/button {:variant :outlined
                 :on-click #(rf/dispatch [::model/hide-dialog])} "cancel"]
    [mui/button {:variant :outlined :type :submit} "save"]]])

(defn dialog []
  (let [dialog (<< ::model/dialog)
        default-props {:open (or (:open dialog) false)
                       :maxWidth false
                       :scroll :paper}
        case-id (<< ::case-model/case-id)]
    (when (:open dialog)
      (let [property (model/get-property (:id dialog))]
        property
        (case (:dialog-type dialog)
          :edit
          [mui/dialog default-props
           [form/form {:layout edit
                       :submit-fn #(rf/dispatch [::model/edit-property case-id (:id dialog) %])
                       :initial-values property}]]
          :add
          [mui/dialog default-props
           [form/form {:layout add
                       :submit-fn #(rf/dispatch [::model/add-property case-id %])}]])))))