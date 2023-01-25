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
    [re-frame.core :as rf]
    [reagent.format :as format]
    [darbylaw.web.theme :as theme]
    [darbylaw.web.ui.case-commons :as case-commons]
    [reagent-mui.lab.masonry :as mui-masonry]
    [darbylaw.web.ui.bills.add-dialog :as add-bill-dialog]))

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

(defn add-bank []
  [mui/card-action-area {:on-click #(rf/dispatch [::banking-model/show-add-dialog :bank]) :sx {:padding-top "0.5rem"}}
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
    [add-bank]]
   [bank-dialog/base-dialog]])

(defn asset-card [{:keys [title _on-add]} & body]
  [mui/card
   [mui/card-content
    [mui/typography {:variant :h5
                     :sx {:font-weight 600}}
     title]
    [mui/divider]
    (into [:<>] body)]])

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
     (if (number? value)
       [mui/typography {:variant :body1
                        :sx {:font-weight :bold}}
        (str "£" (format-currency value))])]]
   [mui/divider]])

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
     (when-not account
       [asset-add-button
        {:title "add funeral account"
         :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :add-funeral-director])}])
     [asset-add-button
      {:title "add other expense"
       :on-click #(rf/dispatch [::funeral-model/show-funeral-dialog :add-other])}]
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
                        :on-click #(rf/dispatch [::banking-model/show-process-dialog :buildsoc id])}]))
       (:buildsoc-accounts current-case))
     [asset-add-button
      {:title "add building society"
       :on-click #(rf/dispatch [::banking-model/show-add-dialog :buildsoc])}]]))

(defn bills-card []
  [:<>
   [add-bill-dialog/dialog]
   [asset-card {:title "household bills"}
    [asset-add-button
     {:title "add bill"
      :on-click add-bill-dialog/show}]]])

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
       [case-commons/fake-case-chip (:fake current-case)]]]
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
