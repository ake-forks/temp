(ns darbylaw.web.ui.admin
  (:require [darbylaw.web.routes :as routes]
            [clojure.string :as str]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [kee-frame.core :as kf]
            [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]
            [reagent-mui.x.data-grid :refer [data-grid]]
            [darbylaw.web.ui.mailing :as mailing]
            [darbylaw.web.ui.case-commons :as case-commons]
            ["@mui/x-data-grid" :as MuiDataGrid]))


(rf/reg-event-db ::load-success
  (fn [db [_ response]]
    (assoc db :cases response)))

(rf/reg-event-fx ::load-failure
  (fn [_ [_ response]]
    (println "failure" response)))

(rf/reg-event-fx ::load!
  (fn [{:keys [db]} _]
    {:db (update db :config/case-view #(or % :card))
     :http-xhrio
     (ui/build-http
       {:method :get
        :uri "/api/cases"
        :on-success [::load-success]
        :on-failure [::load-failure]})}))

(rf/reg-event-db ::set-case-view
  (fn [db [_ view]]
    (assoc db :config/case-view view)))

(rf/reg-sub ::cases
  (fn [db _]
    (:cases db)))

(rf/reg-sub ::case-view
  (fn [db _]
    (:config/case-view db)))


;; >> Cards

(defn case-item
  [{:keys [id reference fake loading?] :as _case
    {:keys [surname forename postcode]} :personal-representative}]
  [mui/card
   [mui/card-content
    [mui/stack {:direction :row
                :justify-content :space-between
                :align-items :stretch}
     [mui/card-action-area {:href (when-not loading?
                                    (kf/path-for [:dashboard {:case-id id}]))}
      [mui/stack
       [mui/typography {:sx {:fontSize 14} :color :text.secondary}
        (if-not loading?
          (str "case " reference)
          [mui/skeleton {:width 100}])]
       [mui/typography {:variant :h5}
        (if-not loading?
          (str surname ", " forename)
          [mui/skeleton {:width 200}])]
       [mui/typography {:variant :h6 :color :text.secondary}
        (if-not loading?
          (str "at " postcode)
          [mui/skeleton {:width 130}])]]]
     [mui/stack {:justify-content :space-between
                 :align-items :end}
      [mui/box
       (when fake
         (case-commons/fake-case-chip fake))]
      [mui/tooltip {:title "Go to case history"}
       [mui/icon-button {:href (kf/path-for [:case-history {:case-id id}])}
        [ui/icon-history-edu]]
       #_[mui/link {:href (kf/path-for [:case-history {:case-id id}])}
          "history"]]]]]])

(defn no-cases-found
  []
  [mui/alert {:severity :info :sx {:z-index 999}}
   [mui/alert-title "No Cases Found"]
   "Maybe "
   [mui/link {:href (kf/path-for [:create-case])} "create"]
   " a new one?"])

(defn card-list []
  (let [cases @(rf/subscribe [::cases])]
    [mui/container {:max-width :sm}
     [mui/stack {:spacing 2}
      (cond
        (empty? cases)
        [no-cases-found]

        (nil? cases)
        (for [id (range 3)]
          ^{:key id}
          [case-item {:loading? true}])

        :default
        (for [case cases]
          ^{:key (:id case)}
          [case-item case]))]]))


;; >> DataGrid

(defn ->full-name
  [{:keys [forename surname]}]
  (str surname ", " forename))

(defn pr->address
  [pr]
  (->> [(:flat pr)
        (:building pr)
        (:street-number pr)
        (:street1 pr)
        (:street2 pr)
        (:town pr)
        (:postcode pr)]
       (remove str/blank?)
       (str/join "\n")))

;; TODO: Split by group?
(def raw-columns
  [{:field :id :hide true}
   {:field :reference :headerName "Reference"}
   {:field :fake
    :valueGetter #(-> % :row :fake (or false))
    :headerName "Fake?"}

   {:field :pr-name
    :headerName "Name"
    :valueGetter #(-> % :row :personal-representative ->full-name)
    :group "Personal Representative"}
   {:field :pr-address
    :headerName "Address"
    :valueGetter #(-> % :row :personal-representative pr->address)
    :width 200
    :group "Personal Representative"}
   {:field :pr-email
    :headerName "Email"
    :hide true
    :valueGetter #(-> % :row :personal-representative :email)
    :width 200
    :group "Personal Representative"}
   {:field :pr-phone
    :headerName "Phone Number"
    :hide true
    :valueGetter #(-> % :row :personal-representative :phone)
    :group "Personal Representative"}

   {:field :dd-name
    :headerName "Name"
    :valueGetter #(-> % :row :deceased ->full-name)
    :group "The Deceased"}
   {:field :dd-address
    :headerName "Address"
    :valueGetter #(-> % :row :deceased :address)
    :width 200
    :group "The Deceased"}
   {:field :dd-dod
    :headerName "Date of Death"
    :valueGetter #(-> % :row :deceased :date-of-death)
    :group "The Deceased"}
   {:field :dd-dob
    :headerName "Date of Birth"
    :hide true
    :valueGetter #(-> % :row :deceased :date-of-birth)
    :group "The Deceased"}
   {:field :dd-sex
    :headerName "Sex"
    :hide true
    :valueGetter #(-> % :row :deceased :sex)
    :group "The Deceased"}
   {:field :dd-entry-number
    :headerName "Entry Number"
    :hide true
    :valueGetter #(-> % :row :deceased :entry-number)
    :group "The Deceased"}
   {:field :dd-informant
    :headerName "Informant"
    :hide true
    :valueGetter #(-> % :row :deceased :name-of-informant)
    :group "The Deceased"}
   {:field :dd-registration-district
    :headerName "Registration District"
    :hide true
    :valueGetter #(-> % :row :deceased :registration-district)
    :group "The Deceased"}
   {:field :dd-occupation
    :headerName "Occupation"
    :hide true
    :valueGetter #(-> % :row :deceased :occupation)
    :group "The Deceased"}
   {:field :dd-relationship
    :headerName "Relationship"
    :valueGetter #(-> % :row :deceased :relationship)
    :group "The Deceased"}
   {:field :dd-certificate-number
    :headerName "Certificate Number"
    :hide true
    :valueGetter #(-> % :row :deceased :certificate-number)
    :group "The Deceased"}
   {:field :dd-cause-of-death
    :headerName "Cause of Death"
    :hide true
    :valueGetter #(-> % :row :deceased :cause-of-death)
    :group "The Deceased"}
   {:field :dd-certifying-doctor
    :headerName "Certifying Doctor"
    :hide true
    :valueGetter #(-> % :row :deceased :name-of-doctor-certifying)
    :group "The Deceased"}
   {:field :dd-registrar
    :headerName "Registrar"
    :hide true
    :valueGetter #(-> % :row :deceased :name-of-registrar)
    :group "The Deceased"}
   {:field :dd-maiden-name
    :headerName "Maiden Name"
    :hide true
    :valueGetter #(-> % :row :deceased :maiden-name)
    :group "The Deceased"}
   {:field :dd-place-of-death
    :headerName "Place of Death"
    :hide true
    :valueGetter #(-> % :row :deceased :place-of-death)
    :group "The Deceased"}
   {:field :dd-place-of-birth
    :headerName "Place of Birth"
    :hide true
    :valueGetter #(-> % :row :deceased :place-of-birth)
    :group "The Deceased"}

   {:field :ba-names
    :headerName "Name(s)"
    :hide true
    :valueGetter #(->> % :row :bank-accounts (map :bank-id) (str/join ", ")) 
    :width 200
    :group "Bank Accounts"}
   {:field :ba-account-numbers
    :headerName "Account Number(s)"
    :hide true
    :valueGetter #(->> % :row :bank-accounts (mapcat :accounts) (map :account-number) (str/join ", ")) 
    :width 200
    :group "Bank Accounts"}
   {:field :ba-sort-codes
    :headerName "Sort Code(s)"
    :hide true
    :valueGetter #(->> % :row :bank-accounts (mapcat :accounts) (map :sort-code) (str/join ", ")) 
    :width 200
    :group "Bank Accounts"}

   {:field :bsa-names
    :headerName "Name(s)"
    :hide true
    :valueGetter #(->> % :row :buildsoc-accounts (map :buildsoc-id) (str/join ", ")) 
    :width 200
    :group "Building Society Accounts"}
   {:field :bsa-roll-numbers
    :headerName "Roll Number(s)"
    :hide true
    :valueGetter #(->> % :row :buildsoc-accounts (mapcat :accounts) (map :roll-number) (str/join ", ")) 
    :width 200
    :group "Building Society Accounts"}

   {:field :fa-name
    :headerName "Name"
    :hide true
    :valueGetter #(-> % :row :funeral-account :title)
    :group "Funeral Account"}

   {:field :fe-names
    :headerName "Name(s)"
    :hide true
    :valueGetter #(->> % :row :funeral-expense (map :title) (str/join ", ")) 
    :width 200
    :group "Funeral Expenses"}

   {:field :bill-types
    :headerName "Type(s)"
    :hide true
    :valueGetter #(->> % :row :bills (mapcat :bill-type) (remove nil?) (str/join ", ")) 
    :width 200
    :group "Bills"}
   {:field :bill-account-numbers
    :headerName "Account Number(s)"
    :hide true
    :valueGetter #(->> % :row :bills (map :account-number) (remove nil?) (str/join ", ")) 
    :width 200
    :group "Bills"}])

(defn wrap-method [method]
  "Wrap a method so that it receives a clj map instead of a js object"
  (fn [params]
    (method (js->clj params :keywordize-keys true))))

(defn adapt-column
  "Adapt the column so that functions are a bit nicer for clojure"
  [column]
  (->> column
       (map (fn [[k v]]
              [k
               (if (fn? v) (wrap-method v) v)]))
       (into {})))

(def columns
  (->> raw-columns
       (map adapt-column)))

(def column-groups
  (->> columns
       (filter :group)
       (group-by :group)
       (map (fn [[group children]]
              {:groupId group
               :children (->> children
                              (map #(select-keys % [:field])))}))))

(rf/reg-sub ::rows
  :<- [::cases]
  (fn [cases]
    (->> cases (map #(update % :id str)))))

(defn data-grid-list
  []
  (let [rows @(rf/subscribe [::rows])]
    [mui/box {:height 400}
     [data-grid {:loading (nil? rows)
                 :rows rows
                 :columns columns
                 :experimental-features {:columnGrouping true}
                 :column-grouping-model column-groups
                 :density :standard
                 :on-row-click #(rf/dispatch [::ui/navigate [:dashboard {:case-id (-> % .-row .-id str)}]])
                 :is-row-selectable (constantly false)
                 :components {:NoRowsOverlay 
                              #(r/as-element [mui/stack {:height "100%" :align-items :center :justify-content :center}
                                              [no-cases-found]])
                              ;; NOTE: We need to supply the raw React component here and not a wrapped version from Reagent
                              ;;       If we don't we get dropped inputs and other weird behaviour
                              ;;       We're not sure why 🤷
                              :Toolbar MuiDataGrid/GridToolbar}
                 :components-props {:toolbar {:showQuickFilter true}}
                 :sx {"& .MuiDataGrid-row" {:cursor :pointer}}}]]))


;; >> Panel

(defn admin-panel []
  (let [case-view @(rf/subscribe [::case-view])]
    [mui/container

     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :center
                 :sx {:mt "3rem"
                      :mb "1rem"}}
      [mui/typography {:variant :h4} "Cases"]
      [mui/stack {:direction :row
                  :spacing 2}
       [mui/button {:startIcon (r/as-element [ui/icon-add])
                    :href (str (kf/path-for [:create-case]) "?fake=true")
                    :variant :outlined}
        "Create fake case"]
       [mui/button {:startIcon (r/as-element [ui/icon-add])
                    :href (kf/path-for [:create-case])
                    :variant :outlined}
        "Create real case"]]]
     [mui/box {:border-bottom 1 :border-color :divider}
      [mui/tabs {:value (or case-view :card)
                 :on-change (fn [_ value] (rf/dispatch [::set-case-view (keyword value)]))}
       [mui/tab {:label "List" :value :card}]
       [mui/tab {:label "Table" :value :data-grid}]
       [mui/tab {:label "Mailing" :value :mail}]]]
     [mui/box {:margin-top 1}
      (case (or case-view :card)
        :card [card-list]
        :data-grid [data-grid-list]
        :mail [mailing/panel])]]))

(defn panel []
  (rf/dispatch [::load!])
  [admin-panel])

(defmethod routes/panels :admin-panel []
  [panel])
