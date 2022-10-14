(ns darbylaw.web.ui.deceased-details
  (:require [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]
            [fork.re-frame :as fork]
            [reagent-mui.components :as mui]
            [darbylaw.web.ui :as ui]
            [reagent.core :as r]))

(rf/reg-event-fx ::add-to-case-success
  (fn [{:keys [db]} [_ case-id {:keys [path]} response]]
    {:db (fork/set-submitting db path false)
     ::ui/navigate [:case {:case-id case-id}]}))

(rf/reg-event-fx ::add-to-case-failure
  (fn [{:keys [db]} [_ {:keys [path]} response]]
    {:db (fork/set-submitting db path false)}))

(rf/reg-event-fx ::add-to-case
  (fn [_ [_ case-id {:keys [values] :as fork-params}]]
    {:http-xhrio
     (ui/build-http
       {:method :patch
        :uri (str "/api/case/" case-id)
        :params {:deceased values}
        :on-success [::add-to-case-success case-id fork-params]
        :on-failure [::add-to-case-failure fork-params]})}))

(rf/reg-event-fx ::submit!
  (fn [{:keys [db]} [_ case-id {:keys [path] :as fork-params}]]
    {:db (fork/set-submitting db path true)
     :dispatch [::add-to-case case-id fork-params]}))

(defn deceased-details-form [{:keys [handle-submit
                                     submitting?
                                     values]
                              :as fork-args}]
  [:form
   {:on-submit handle-submit}
   [mui/stack {:spacing 1}


    [mui/stack {:direction :row :spacing 1}

     [mui/typography {:variant :h6} "Who was the deceased to you?"]
     [mui/form-control {:required true :full-width true}
      [mui/input-label {:id :relationship-select} "Relationship"]
      [mui/select {:label "Relationship"
                   :labelId :relationship-select
                   :name :relationship
                   :value (:relationship values)
                   ;:onChange (ui/form-handle-change-fn fork-args)
                   :variant :filled}
       [mui/menu-item {:disabled true :value "" :key :placeholder} "I'm filling out this form on behalf of my late..."]
       [mui/menu-item {:value "Mother" :key :Mother} "Mother"]
       [mui/menu-item {:value "Father" :key :Father} "Father"]
       [mui/menu-item {:value "Grandmother" :key :Grandmother} "Grandmother"]
       [mui/menu-item {:value "Grandfather" :key :Grandfather} "Grandfather"]

       [mui/menu-item {:value "Wife" :key :Wife} "Wife"]
       [mui/menu-item {:value "Husband" :key :Husband} "Husband"]
       [mui/menu-item {:value "Sister" :key :Sister} "Sister"]
       [mui/menu-item {:value "Brother" :key :Brother} "Brother"]
       [mui/menu-item {:value "Child" :key :Child} "Child"]
       [mui/menu-item {:value "Cousin" :key :Cousin} "Cousin"]
       [mui/menu-item {:value "Aunt" :key :Aunt} "Aunt"]
       [mui/menu-item {:value "Uncle" :key :Uncle} "Uncle"]
       [mui/menu-item {:value "Stepparent" :key :Stepparent} "Step-parent"]
       [mui/menu-item {:value "Friend" :key :Friend} "Friend"]
       [mui/menu-item {:value "Other" :key :Other} "Other"]]]]


    [mui/typography {:variant :h5} "deceased's details"]

    [mui/stack {:direction :row
                :spacing 1}

     [mui/text-field {:label "Their Forename"
                      :required true
                      :placeholder "Please enter their legal name"
                      :name :forename
                      :value (:forename values)
                      ;:onChange (ui/form-handle-change-fn fork-args)
                      :full-width true
                      :variant :filled}]
     [mui/text-field {:label "Their Middle Name(s)"
                      :name :middlename
                      :value (:middlename values)
                      ;:onChange (ui/form-handle-change-fn fork-args)
                      :full-width true
                      :variant :filled}]]

    [mui/stack {:direction :row
                :spacing 1}
     [mui/text-field {:label "Their Surname"
                      :required true
                      :name :surname
                      :value (:surname values)
                      ;:onChange (ui/form-handle-change-fn fork-args)
                      :style {:flex-grow 2}
                      :variant :filled}]

     [mui/text-field {:label "Maiden Name/Surname at Birth"
                      :name :birth-surname
                      :value (:birth-surname values)
                      ;:onChange (ui/form-handle-change-fn fork-args)
                      :style {:flex-grow 1}
                      :variant :filled}]]



    [mui/stack {:direction :row
                :spacing 1}
     [mui/stack {:spacing 1 :sx {:width "100%"}}

      [mui/text-field {:label "Date of Birth"
                       :helper-text "Please use format DD/MM/YYYY"

                       :required true
                       :name :dob
                       :value (:dob values)
                       ;:onChange (ui/form-handle-change-fn fork-args)
                       :full-width true
                       :variant :filled}]]
     [mui/stack {:spacing 1 :sx {:width "100%"}}


      [mui/text-field {:label "Place of Birth"
                       :required true
                       :name :pob
                       :value (:pob values)
                       ;:onChange (ui/form-handle-change-fn fork-args)
                       :full-width true
                       :variant :filled}]]]

    [mui/stack {:direction :row :spacing 1}
     [mui/text-field {:label "Deceased's Occupation"
                      :required true
                      :name :occupation
                      :value (:occupation values)
                      ;:onChange (ui/form-handle-change-fn fork-args)
                      :style {:flex-grow 2}
                      :variant :filled}]

     [mui/form-control {:style {:flex-grow 1}}
      [mui/input-label {:id :sex-select} "Legal Sex"]
      [mui/select {:label "Legal Sex"
                   :labelId :sex-select
                   :name :sex
                   :value (:sex values)
                   ;:onChange (ui/form-handle-change-fn fork-args)
                   :variant :filled}
       [mui/menu-item {:value "Male" :key :Male} "Male"]
       [mui/menu-item {:value "Female" :key :Female} "Female"]]]]



    [mui/text-field {:label "Usual Address"
                     :name :address
                     :value (:address values)
                     ;:onChange (ui/form-handle-change-fn fork-args)
                     :multiline true
                     :min-rows 3
                     :variant :filled}]


    [mui/stack {:direction :row
                :spacing 1}
     [mui/stack {:spacing 1 :sx {:width "100%"}}
      [mui/input-label {:id :dod-input} "Deceased's Date of Death"]
      [mui/text-field {:label "DD/MM/YYYY"
                       :labelId :dod-input
                       :required true
                       :name :dod
                       :value (:dod values)
                       ;:onChange (ui/form-handle-change-fn fork-args)
                       :full-width true
                       :variant :filled}]]
     [mui/stack {:spacing 1 :sx {:width "100%"}}
      [mui/input-label {:id :pod-input} "Place of Death"]
      [mui/text-field {:label "Place of Death"
                       :labelId :pod-input
                       :required true
                       :name :pod
                       :value (:pod values)
                       ;:onChange (ui/form-handle-change-fn fork-args)
                       :full-width true
                       :variant :filled}]]]

    [ui/???_TO_BE_DEFINED_??? "do we need? occupation, sex, address."]
    [ui/???_TO_BE_DEFINED_??? "do we add? cause of death, date registered, death certificate number"]
    [mui/button {:variant :contained
                 :type :submit
                 :disabled submitting?}
     "Create Case"]]])

(defonce form-state (r/atom nil))

(defn panel []
  (let [route-params @(rf/subscribe [::routes/route-params])] ;route-params = UUID of case
    [mui/container {:max-width :md}
     [fork/form
      {:state form-state
       :on-submit (do
                    (assert (:case-id route-params))
                    #(rf/dispatch [::submit! (:case-id route-params) %])) ;% = :state :path :values :dirty :reset
       :keywordize-keys true
       :prevent-default? true
       :initial-values {:relationship ""
                        :sex ""}}
      (fn [fork-args]
        [deceased-details-form fork-args])]]))

(defmethod routes/panels :deceased-details-panel [] [panel])

(comment
  ; To fill out the form programmatically:
  (do
    (def test-data
      {:forename "Richard",
       :sex "Male",
       :occupation "Politician",
       :dod "12/12/1975",
       :relationship "Father",
       :surname "Roe",
       :middlename "J",
       :dob "01/01/1905",
       :pod "Edimbourg",
       :pob "London"},)
    (swap! form-state assoc :values test-data)
    (darbylaw.web.core/mount-root)))