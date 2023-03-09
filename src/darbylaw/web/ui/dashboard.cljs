(ns darbylaw.web.ui.dashboard
  (:require
    [clojure.string :as str]
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.app-layout :as c]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.dialog :as funeral-dialog]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.banking.model :as banking-model]
    [darbylaw.web.ui.banking.dialog :as banking-dialog]
    [darbylaw.web.ui.bank-dialog :as bank-dialog]
    [darbylaw.api.bank-list :as bank-list]
    [darbylaw.web.ui.progress-bar :as progress-bar]
    [darbylaw.web.ui.overview-tile :as overview]
    [darbylaw.web.ui.tasks-tile :as tasks]
    [darbylaw.web.ui.keydocs.dialog :as key-docs]
    [darbylaw.web.ui.identity.dialog :as identity-dialog]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.case-commons :as case-commons]
    [reagent-mui.lab.masonry :as mui-masonry]
    [darbylaw.web.ui.bills.add-dialog :as add-bill-dialog]
    [darbylaw.web.ui.bills.bills-dialog :as bills-dialog]
    [darbylaw.web.ui.bills.council-tax-dialog :as council-tax]
    [darbylaw.web.ui.bills.model :as bills-model]
    [medley.core :as medley]
    [darbylaw.api.util.data :as data-util]
    [darbylaw.web.ui.notification.dialog :as notification-dialog]
    [darbylaw.web.ui.notification.model :as notification-model]))

(defn bank-item [bank]
  (let [bank-data (bank-list/bank-by-id (:bank-id bank))
        bank-id (:bank-id bank)
        accounts (:accounts bank)]
    [mui/box
     [mui/card-action-area {:on-click #(rf/dispatch [::banking-model/show-process-dialog :bank bank-id])
                            :sx {:padding-top "0.5rem" :padding-bottom "0.5rem"}}
      [mui/stack {:direction :row :spacing 1}
       [mui/box {:component :img
                 :src (str "/images/bank-logos/" (:logo bank-data))
                 :sx {:width 25 :mr 1}}]
       [mui/stack {:spacing 0.5 :direction :row :justify-content :space-between :style {:width "100%"}}
        [mui/typography {:variant :body1} (:common-name bank-data)]]
       [mui/typography {:variant :body1 :sx {:font-weight :bold}}
        (->> accounts
          (map #(if-let [confirmed-value (:confirmed-value %)]
                  confirmed-value
                  (:estimated-value %)))
          (map #(if (str/blank? %)
                  0
                  (js/parseFloat %)))
          (reduce +)
          (format/format "%.2f")
          (str "£"))]]]
     [mui/divider {:variant "middle"}]]))

(defn asset-add-button [{:keys [title on-click]}]
  [mui/card-action-area {:on-click on-click
                         :sx {:padding-top 1}}
   [mui/stack {:direction :row
               :spacing 0.5
               :align-items :center
               :style {:color theme/teal}}
    [mui/typography {:style {:font-weight 500}}
     (or title "add")]
    [ui/icon-add {:font-size :small}]]])

(defn asset-card [{:keys [title _on-add]} & body]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5
                     :sx {:font-weight 600}}
     title]
    [mui/divider]
    (into [:<>] body)]])

(defn bank-card [current-case]
  [asset-card {:title "bank accounts"}
   [bank-dialog/base-dialog]
   (for [bank (:bank-accounts current-case)]
     ^{:key (:bank-id bank)}
     [bank-item bank])
   [asset-add-button
    {:title "add"
     :on-click #(rf/dispatch [::banking-model/show-add-dialog :bank])}]])

(defn menu-asset-add-button
  "anchor = atom
  options = a map of option labels and their on-click functions as key-value pairs"
  [anchor options]
  [:<>
   [asset-add-button 
    {:title "add"
     :on-click #(reset! anchor (.-target %))}]
   [mui/menu {:open (some? @anchor)
              :anchor-el @anchor
              :on-close #(reset! anchor nil)
              :anchor-origin {:vertical "bottom"
                              :horizontal "left"}
              :transform-origin {:vertical "top"
                                 :horizontal "left"}}
    (map (fn [[k v]]
           ^{:key k}
           [mui/menu-item {:on-click v} k])
      options)]])

(defn format-currency
  [value]
  (format/format "%.2f" value))

(defn asset-item [{:keys [title value on-click icon indent no-divider]}]
  [mui/box
   [mui/card-action-area {:on-click on-click
                          :sx {:padding-top 1 :padding-bottom 1}}
    [mui/stack (merge
                 {:direction :row
                  :spacing 2
                  :justify-content :space-between}
                 (when indent
                   {:sx {:pl (* indent 2)}}))
     (if (string? icon)
       [mui/box {:component :img
                 :src icon
                 :sx {:width 25 :mr 1}}]
       icon)
     [mui/typography {:variant :body1
                      :noWrap true
                      :sx {:width "100%"}}
      title]
     (when (number? value)
       [mui/typography {:variant :body1
                        :sx {:font-weight :bold}}
        (str "£" (format-currency value))])]]
   (when-not no-divider
     [mui/divider])])

;I think we should move these cards to their respective asset areas (like we have for tasks)
(defn funeral-card []
  (let [dialog-info @(rf/subscribe [::funeral-model/dialog-info])
        account @(rf/subscribe [::funeral-model/account])
        expenses @(rf/subscribe [::funeral-model/expense-list])]
    [asset-card {:title "funeral expenses"}
     (when account
       [asset-item
        {:title (:title account)
         :value (js/parseFloat (:value account))
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :edit-account])
         ;; TODO: Make right size
         :icon [mui/skeleton {:variant :circular
                              :width 25}]}])
     (for [{:keys [expense-id value title]} expenses]
       ^{:key expense-id}
       [asset-item
        {:title title
         :value (js/parseFloat value)
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog expense-id])
         ;; TODO: Make right size
         :icon [mui/skeleton {:variant :circular
                              :width 25}]}])
     (if account
       [asset-add-button
        {:title "add"
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :add-other])}]
       [menu-asset-add-button (r/atom nil)
        {"add funeral account" #(rf/dispatch [::funeral-model/show-funeral-dialog :add-funeral-director])
         "add other expense" #(rf/dispatch [::funeral-model/show-funeral-dialog :add-other])}])
     (when dialog-info
       [funeral-dialog/main-dialog])]))

(defn buildsoc-card []
  (let [current-case @(rf/subscribe [::case-model/current-case])]
    [asset-card {:title "building societies"}
     [banking-dialog/dialog]
     (map
       (fn [buildsoc]
         (let [id (:buildsoc-id buildsoc)]
           ^{:key id}
           [asset-item {:title (banking-model/asset-label :buildsoc id)
                        :value (->> buildsoc
                                 :accounts
                                 (map #(if-let [confirmed-value (:confirmed-value %)]
                                         confirmed-value
                                         (:estimated-value %)))
                                 (map #(if (str/blank? %)
                                         0
                                         (js/parseFloat %)))
                                 (reduce +))
                        :on-click #(rf/dispatch [::banking-model/show-process-dialog :buildsoc id])
                        :icon (str "/images/buildsoc-logos/" (banking-model/get-logo :buildsoc id))}]))
       (:buildsoc-accounts current-case))
     [asset-add-button
      {:title "add"
       :on-click #(rf/dispatch [::banking-model/show-add-dialog :buildsoc])}]]))

(defn bills-card []
  (let [current-case @(rf/subscribe [::case-model/current-case])
        utilities-by-property-id (group-by :property (:utilities current-case))
        council-tax-by-property-id (group-by :property (:council-tax current-case))
        used-property-ids (->> (concat (:council-tax current-case)
                                       (:utilities current-case))
                            (keep :property)
                            (distinct))
        properties-by-id (medley/index-by :id (:properties current-case))
        company-id->label @(rf/subscribe [::bills-model/company-id->label])
        council-id->label @(rf/subscribe [::bills-model/council-id->label])]
    [:<>
     [add-bill-dialog/panel]
     [bills-dialog/dialog]
     [council-tax/panel]
     [notification-dialog/dialog]
     [asset-card {:title "household bills"}
      (for [property-id used-property-ids]
        [:<> {:key property-id}
         [mui/typography {:sx {:font-weight 600
                               :pt 1}}
          (if-let [address (get-in properties-by-id [property-id :address])]
            (data-util/first-line address)
            "[unknown address]")]
         (for [utility-company (->> (get utilities-by-property-id property-id)
                                 (map #(:utility-company %))
                                 (distinct)
                                 (remove nil?))]

           ^{:key utility-company}
           [asset-item {:title (if (keyword? utility-company)
                                 (company-id->label utility-company)
                                 utility-company)
                        :on-click #(rf/dispatch [::notification-model/open
                                                 {:notification-type :utility
                                                  :case-id (:id current-case)
                                                  :utility-company utility-company
                                                  :property property-id}])
                        :indent 1
                        :no-divider true}])

         (for [council (->> (get council-tax-by-property-id property-id)
                         (map #(:council %))
                         (distinct)
                         (remove nil?))]
           ^{:key council}
           [asset-item {:title (if (keyword? council)
                                 (council-id->label council)
                                 council)
                        :on-click #(rf/dispatch [::notification-model/open
                                                 {:notification-type :council-tax
                                                  :case-id (:id current-case)
                                                  :council council
                                                  :property property-id}])
                        :indent 1
                        :no-divider true}])
         [mui/divider]])
      [menu-asset-add-button bills-model/bills-dashboard-menu
       {"add utility" #(rf/dispatch [::bills-model/show-bills-dialog
                                     {:service :utility
                                      :id nil
                                      :dialog :add}])
        "add council tax" #(rf/dispatch [::bills-model/show-bills-dialog
                                         {:service :council-tax
                                          :id nil
                                          :dialog :add}])}]]]))

(defn heading [current-case]
  [mui/box {:sx {:background-color theme/off-white :padding-bottom {:xs "2rem" :xl "4rem"}}}

   [mui/container {:maxWidth :xl :class (styles/main-content)}
    [mui/stack {:spacing 3}
     [mui/stack {:direction :row
                 :justify-content :space-between
                 :align-items :baseline
                 :sx {:padding-top {:xs "0.5rem" :xl "2rem"}}}
      [mui/typography {:variant :h4}
       (if (nil? (:deceased current-case))
         (str "welcome")
         (str "your "
           (-> current-case :deceased :relationship (clojure.string/lower-case))
           "'s estate"))]
      [mui/stack {:direction :row
                  :spacing 1
                  :align-items :center}
       [mui/typography {:variant :h6}
        (if (nil? current-case)
          [mui/skeleton {:width "5rem"}]
          (str "case " (:reference current-case :reference)))]
       (when (:fake current-case)
         [case-commons/fake-case-chip (:fake current-case)])]]
     [progress-bar/progress-bar]]]])

(defn content [current-case]
  [mui/container {:maxWidth :xl}
   [mui/stack {:spacing 2 :sx {:pt "1rem" :pb "2rem"}}
    [mui/typography {:variant :h5} "estate details"]
    [mui/stack {:direction :row :spacing 1 :style {:margin-top "0.5rem"}}
     [mui-masonry/masonry {:columns 3}
      [bank-card current-case]
      [buildsoc-card]
      [bills-card]
      [funeral-card current-case]]
     [mui/stack {:spacing 2 :style {:width "30%"}}
      [tasks/tasks-tile]
      [key-docs/dash-button]
      [key-docs/dialog]
      [identity-dialog/dialog]
      [overview/overview-card]]]]])

(defn panel* []
  (let [current-case @(rf/subscribe [::case-model/current-case])]
    [mui/box
     [c/navbar]
     [heading current-case]
     [content current-case]
     [c/footer]]))

(defn panel []
  (let [case-id @(rf/subscribe [::case-model/case-id])]
    (assert case-id)
    (rf/dispatch [::case-model/load-case! case-id])
    [panel*]))

(defmethod routes/panels :dashboard-panel [] [panel])
