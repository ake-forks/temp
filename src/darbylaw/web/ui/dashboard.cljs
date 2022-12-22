(ns darbylaw.web.ui.dashboard
  (:require
    [reagent-mui.components :as mui]
    [darbylaw.web.ui.app-layout :as c]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.styles :as styles]
    [darbylaw.web.routes :as routes]
    [darbylaw.web.ui.funeral.model :as funeral-model]
    [darbylaw.web.ui.funeral.dialog :as funeral-dialog]
    [darbylaw.web.ui.bank-model :as bank-model]
    [darbylaw.web.ui.case-model :as case-model]
    [darbylaw.web.ui.buildingsociety.model :as buildsoc-model]
    [darbylaw.web.ui.buildingsociety.dialog :as buildsoc-dialog]
    [darbylaw.web.ui.bank-dialog :as bank-dialog]
    [darbylaw.api.bank-list :as bank-list]
    [darbylaw.web.ui.progress-bar :as progress-bar]
    [darbylaw.web.ui.overview-tile :as overview]
    [darbylaw.web.ui.tasks-tile :as tasks]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]))


(defn bank-item [bank]
  (let [bank-data (bank-list/bank-by-id (:bank-id bank))
        bank-id (:bank-id bank)
        accounts (:accounts bank)
        bank-dialog @(rf/subscribe [::bank-model/bank-dialog])]
    [mui/box
     [mui/card-action-area {:on-click #(rf/dispatch [::bank-model/show-bank-dialog bank-id])
                            :sx {:padding-top "0.5rem" :padding-bottom "0.5rem"}}
      [mui/stack {:direction :row :spacing 1}
       [mui/box {:component :img
                 :src (str "/images/bank-logos/" (:icon bank-data))
                 :sx {:width 25 :mr 1}}]
       [mui/stack {:spacing 0.5 :direction :row :justify-content :space-between :style {:width "100%"}}
        [mui/typography {:variant :body1} (:common-name bank-data)]]
       [mui/typography {:variant :body1 :sx {:font-weight :bold}}
        (str "£" (format/format "%.2f"
                   (reduce + (map (fn [account]
                                    (if (clojure.string/blank? (:estimated-value account))
                                      0
                                      (js/parseFloat (:estimated-value account)))) accounts))))]]]

     [mui/dialog
      {:open (if (= bank-dialog bank-id) true false)
       :maxWidth false
       :fullWidth false}
      [bank-dialog/base-dialog]]

     [mui/divider {:variant "middle"}]]))

(defn add-bank []
  [mui/card-action-area {:on-click #(rf/dispatch [::bank-model/show-bank-dialog :add-bank]) :sx {:padding-top "0.5rem"}}
   [mui/stack {:direction :row :spacing 2 :align-items :baseline}
    [mui/typography {:variant :h5} "add bank account"]
    [ui/icon-add]]])

(defn bank-card [current-case]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:font-weight 600}} "bank accounts"]
    [mui/divider]
    (for [bank (:bank-accounts current-case)]
      ^{:key (:bank-id bank)}
      [bank-item bank])
    [add-bank]]])

(defn asset-card [{:keys [title on-add]} & body]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5
                     :sx {:font-weight 600}}
     title]
    [mui/divider]
    body]])

(defn asset-add-button [{:keys [title on-click]}]
  [mui/card-action-area {:on-click on-click
                         :sx {:padding-top 1}}
   [mui/stack {:direction :row
               :spacing 2
               :align-items :center}
    [mui/typography {:variant :h5}
     (or title "add")]
    [ui/icon-add]]])

(defn format-currency
  [value]
  (format/format "%.2f" value))

(defn asset-item [{:keys [title value on-click icon]}]
  [mui/box
   [mui/card-action-area {:on-click on-click
                          :sx {:padding-top 1 :padding-bottom 1}}
    [mui/stack {:direction :row
                :spacing 2
                :justify-content :space-between}
     (if (string? icon)
       [mui/box {:component :img
                 :src icon}]
       icon)
     [mui/typography {:variant :body1
                      :noWrap true
                      :sx {:width "100%"}}
      title]
     [mui/typography {:variant :body1
                      :sx {:font-weight :bold}}
      (str "£" (format-currency value))]]]
   [mui/divider]])

(defn funeral-card []
  (let [dialog-info @(rf/subscribe [::funeral-model/dialog-info])
        account @(rf/subscribe [::funeral-model/account])
        expenses @(rf/subscribe [::funeral-model/expense-list])]
    [asset-card {:title "funeral expenses"}
     (when account
       ^{:key :funeral-account}
       [asset-item
        {:title (:title account)
         :value (:value account)
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :edit-account])
         ;; TODO: Make right size
         :icon [mui/skeleton {:variant :circular
                              :width 25}]}])
     (for [{:keys [id value title]} expenses]
       ^{:key id}
       [asset-item
        {:title title
         :value value
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog id])
         ;; TODO: Make right size
         :icon [mui/skeleton {:variant :circular
                              :width 25}]}])
     (when-not account
       ^{:key :add-account-button}
       [asset-add-button
        {:title "add funeral account"
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :add-funeral-director])}])
     ^{:key :add-expense-button}
     [asset-add-button
      {:title "add other expense"
       :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :add-other])}]
     (when dialog-info
       ^{:key :funeral-dialog}
       [funeral-dialog/main-dialog])]))

(defn buildsoc-card []
  [asset-card {:title "building societies"}
   [buildsoc-dialog/dialog]
   (map
     (fn [buildsoc]
       (let [id (:buildsoc-id buildsoc)]
         [asset-item {:title (:common-name buildsoc)
                      :value 100
                      :on-click #(rf/dispatch [::buildsoc-model/show-process-dialog id])}]))
     buildsoc-model/buildsoc-accounts)
   [asset-add-button
    {:title "add building society"
     :on-click #(rf/dispatch [::buildsoc-model/show-add-dialog])}]])


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
      [mui/typography {:variant :h6}
       (if (nil? current-case)
         [mui/skeleton {:width "5rem"}]
         (str "case " (:reference current-case :reference)))]]
     [progress-bar/progress-bar]]]])

(defn content [current-case]
  (let [bank-modal @(rf/subscribe [::bank-model/bank-dialog])]
    [mui/container {:maxWidth :xl}

     [mui/stack {:spacing 2 :sx {:pt "1rem" :pb "2rem"}}
      [mui/typography {:variant :h5} "estate details"]
      [mui/dialog
       {:open (= bank-modal :add-bank)
        :maxWidth :md
        :fullWidth true}
       [bank-dialog/base-dialog]]

      [mui/stack {:direction :row :spacing 1 :style {:margin-top "0.5rem"}}
       [mui/grid {:container true :spacing 1 :columns 3
                  :style {:width "70%"}}
        [mui/grid {:item true :xs 1}
         [bank-card current-case]]
        [mui/grid {:item true :xs 1}
         [funeral-card current-case]]
        [mui/grid {:item true :xs 1}
         [buildsoc-card]]]

       [mui/stack {:spacing 2 :style {:width "30%"}}
        [tasks/tasks-tile]
        [overview/overview-card]]]]]))

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
