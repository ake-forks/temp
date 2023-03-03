(ns darbylaw.web.ui.bills.bills-dialog
  (:require
    [clojure.string :as str]
    [darbylaw.web.ui :as ui]
    [darbylaw.web.ui.bills.common :as common]
    [darbylaw.web.util.date :as date-util]
    [medley.core :as medley]
    [re-frame.core :as rf]
    [reagent-mui.components :as mui]
    [reagent.core :as r]
    [darbylaw.web.ui.bills.model :as model]
    [darbylaw.api.bill.data :as bill-data]
    [darbylaw.web.ui.case-model :as case-model]))

(rf/reg-event-db ::set-dialog-open
  (fn [db [_ dialog-context]]
    (if (some? dialog-context)
      (merge db {::dialog-open? true
                 ::dialog-context dialog-context})
      (assoc db ::dialog-open? false))))

(rf/reg-sub ::form-submitting?
  (fn [db]
    (::form-submitting? db)))

(rf/reg-sub ::dialog-open?
  (fn [db]
    (or (::dialog-open? db)
        (::form-submitting? db))))

(rf/reg-sub ::dialog-context
  (fn [db]
    (::dialog-context db)))

(rf/reg-sub ::notification-process
  :<- [::case-model/current-case]
  :<- [::dialog-context]
  (fn [[case-data dialog-context]]
    (->> (:notification-process case-data)
      (filter #(= dialog-context (select-keys % [:asset-type
                                                 :utility-company
                                                 :property])))
      first)))

(rf/reg-sub ::notification-ongoing
  :<- [::notification-process]
  (fn [process]
    (:ready-to-start process)))


(def form-state (r/atom nil))

(defn set-submitting [db submitting?]
  (assoc db ::form-submitting? submitting?))

(rf/reg-event-fx ::start-notification-process-success
  (fn [{:keys [db]} [_ case-id _response]]
    {:db (-> db
           (set-submitting false)
           (assoc ::dialog-open? false))
     ; Should we wait until case is loaded to close the dialog?
     :dispatch [::case-model/load-case! case-id]}))

(rf/reg-event-fx ::start-notification-process-failure
  (fn [{:keys [db]} [_ error-result]]
    {:db (set-submitting db false)
     ::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::start-notification-process
  (fn [{:keys [db]} [_ case-id context]]
    {:db (set-submitting db true)
     :http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/start-notification-process")
        :params (merge {:asset-type :utility-bill}
                       context)
        :on-success [::start-notification-process-success case-id]
        :on-failure [::start-notification-process-failure]})}))

(rf/reg-sub ::issuer-label
  :<- [::model/company-id->label]
  :<- [::dialog-context]
  (fn [[company-id->label {:keys [utility-company]}]]
    (if (keyword? utility-company)
      (company-id->label utility-company)
      utility-company)))

(rf/reg-sub ::dialog-property
  :<- [::dialog-context]
  :<- [::model/current-properties-by-id]
  (fn [[context properties-by-id]]
    (get properties-by-id (:property context))))

(rf/reg-sub ::property-address
 :<- [::dialog-property]
  (fn [property]
    (:address property)))

(rf/reg-sub ::dialog-utilities
  :<- [::model/current-utilities]
  :<- [::dialog-context]
  (fn [[all-utilities {:keys [utility-company property]}]]
    (->> all-utilities
      (filter (fn [utility]
                (and (= utility-company (:issuer utility))
                     (= property (:property utility))))))))

(defn services-str [services]
  (->> bill-data/utility-services
    (keep (fn [[k {:keys [label]}]]
            (when (contains? services k)
              label)))
    (str/join " & ")))

(defn utility-row [data]
  [mui/card {:elevation 2}
   [mui/card-content
    [mui/stack {:direction :row
                :spacing 2
                :sx {:align-items :center}}
     [mui/stack {:sx {:flex-grow 1}}
      [mui/typography {:variant :subtitle} [:b (services-str (:services data))]]
      (when-let [account-number (:account-number data)]
        [mui/typography {:variant :subtitle2} "account: " account-number])]
     [mui/typography {:variant :h6} (str "£" (:amount data))]
     [mui/box
      [mui/icon-button
       [ui/icon-edit]]
      [mui/icon-button
       [ui/icon-delete]]]]]])

(rf/reg-event-fx ::generate-notification-letter-success
  (fn [{:keys [db]} [_ case-id context]]
    {:dispatch [::load-conversation case-id context]}))

(rf/reg-event-fx ::generate-notification-letter-failure
  (fn [{:keys [db]} [_ error-result]]
    {::ui/notify-user-http-error {:result error-result}}))

(rf/reg-event-fx ::generate-notification-letter
  (fn [{:keys [db]} [_ case-id context]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/generate-notification-letter")
        :params (merge {:notification-type :utility}
                       context)
        :timeout 16000
        :on-success [::generate-notification-letter-success case-id context]
        :on-failure [::generate-notification-letter-failure]})}))

(def anchor (r/atom nil))

(defn create-menu []
  (let [close-menu! #(reset! anchor nil)
        case-id @(rf/subscribe [::case-model/case-id])
        context @(rf/subscribe [::dialog-context])]
    [mui/menu {:open (some? @anchor)
               :anchorEl @anchor
               :onClose close-menu!
               :anchorOrigin {:vertical :bottom
                              :horizontal :right}
               :transformOrigin {:vertical :top
                                 :horizontal :right}
               :PaperProps {:sx {:border-radius 0}}}
     [mui/menu-item {:on-click #(do (close-menu!)
                                    (rf/dispatch [::generate-notification-letter case-id context]))}
      [mui/list-item-text
       "create notification letter"]]
     [mui/menu-item {:on-click close-menu!}
      [mui/list-item-text
       "upload received letter"]]]))

(rf/reg-event-fx ::load-conversation-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db ::conversation data)}))

(rf/reg-event-fx ::load-conversation-failure
  (fn [{:keys [db]} [_ error-result]]
    {::ui/notify-user-http-error {:message "Conversation could not be loaded."
                                  :result error-result}}))

(rf/reg-event-fx ::load-conversation
  (fn [{:keys [db]} [_ case-id context]]
    {:http-xhrio
     (ui/build-http
       {:method :post
        :uri (str "/api/case/" case-id "/conversation")
        :params (merge {:notification-type :utility}
                       context)
        :on-success [::load-conversation-success]
        :on-failure [::load-conversation-failure]})}))

(rf/reg-event-fx ::dispose-conversation
  (fn [{:keys [db]} _]
    {:db (dissoc db ::conversation)}))

(rf/reg-sub ::conversation
  (fn [db]
    (::conversation db)))

(rf/reg-event-db ::show-letter
  (fn [db [_ letter-id]]
    (assoc db ::show-letter letter-id)))

(rf/reg-sub ::show-letter
  (fn [db]
    (::show-letter db)))

(rf/reg-sub ::letter-by-id
  :<- [::conversation]
  (fn [conversation]
    (medley/index-by :xt/id conversation)))

(rf/reg-sub ::show-letter-data
  :<- [::letter-by-id]
  :<- [::show-letter]
  (fn [[letter-by-id show-letter]]
    (get letter-by-id show-letter)))

(comment
  (let [case-id @(rf/subscribe [::case-model/case-id])
        context @(rf/subscribe [::dialog-context])]
    (rf/dispatch [::load-conversation case-id context])))

(defn conversation []
  (r/with-let [_ (rf/dispatch [::load-conversation
                               @(rf/subscribe [::case-model/case-id])
                               @(rf/subscribe [::dialog-context])])]
    (let [conversation-data @(rf/subscribe [::conversation])]
      [mui/container {:maxWidth :sm
                      :sx {:height "100%"}}
       [mui/stack {:sx {:height "100%"}}
        [mui/dialog-title
         [mui/stack {:direction :row
                     :sx {:justify-content :space-between}}
          "conversation"
          [mui/button {:startIcon (r/as-element [ui/icon-add])
                       :variant :contained
                       :onClick #(reset! anchor (ui/event-currentTarget %))}
           "create"]
          [create-menu]]]
        [mui/dialog-content
         [mui/paper {:variant :outlined
                     :square true
                     :sx {:overflow :auto
                          :height "100%"
                          :background-color :grey.200}}
          [mui/list {:sx {:py 0}}
           (for [letter conversation-data]
             [:<> {:key (:xt/id letter)}
              [mui/list-item {:sx {:background-color :background.paper
                                   :cursor :pointer}
                              :onClick #(rf/dispatch [::show-letter (:xt/id letter)])}
               [mui/list-item-icon
                [ui/icon-description-outlined]]
               [mui/list-item-text
                {:primary "notification letter"
                 :secondary "in preparation"}]
               [mui/list-item-text
                {:secondary (date-util/show-date-local-numeric (:modified-at letter))
                 :sx {:flex-grow 0}}]]
              [mui/divider]])]]]]])))

(def edit-anchor (r/atom nil))

(defn edit-popover []
  (let [close! #(reset! edit-anchor nil)]
    [mui/popover {:open (some? @edit-anchor)
                  :anchorEl @edit-anchor
                  :onClose close!
                  :anchorOrigin {:vertical :bottom
                                 :horizontal :right}
                  :transformOrigin {:vertical :top
                                    :horizontal :right}
                  :PaperProps {:sx {:border-radius 0}}}
     [mui/container {:maxWidth :sm
                     :sx {:my 2}}
      [mui/typography {:variant :body1
                       :font-weight :bold}
       (cond
         ;(= author :unknown-user)
         ;"This notification letter was modified by a user."
         ;
         ;(string? author)
         ;(str "This notification letter was modified by '" author "'.")

         :else
         "This letter was automatically generated from case data.")]
      [ui/loading-button {:onClick #(do (close!)
                                        (rf/dispatch [::TODO] #_[::regenerate asset-type case-id asset-id letter-id]))
                          :loading false ;@regenerating?
                          :startIcon (r/as-element [ui/icon-refresh])
                          :variant :outlined
                          :sx {:mt 1}}
       "Regenerate letter from current case data"]
      [mui/typography {:variant :body1
                       :sx {:mt 2}}
       "You can modify the letter using Word."]
      [mui/typography {:variant :body2}
       "(Be careful in keeping the first page layout intact, "
       "as the address must match the envelope's window)."]
      [mui/stack {:direction :row
                  :spacing 1
                  :sx {:mt 1}}
       [mui/button {;:href (str "/api/case/" case-id "/" (name asset-type) "/" (name asset-id) "/notification-docx")
                    ;:download (str case-reference " - " (name asset-id) " - notification.docx")
                    :variant :outlined
                    :full-width true
                    :startIcon (r/as-element [ui/icon-download])}
        "download current letter"]
       [mui/button ;shared/upload-button asset-type case-id asset-id
        {:variant :outlined
         :full-width true
         :startIcon (r/as-element [ui/icon-upload])}
        "upload replacement"]]]]))
        ;"/notification-docx"]]]]))

(def confirmation-dialog-open? (r/atom false))
(def override-fake-send? (r/atom false))

(defn send-confirmation-dialog []
  [mui/dialog {:open (boolean @confirmation-dialog-open?)
               :maxWidth :sm
               :fullWidth true}
   [mui/dialog-title "confirm send"]
   [mui/dialog-content
    [mui/box {:sx {:mt 2}}
     [mui/typography {:sx {:mb 1}}
      "This is a " [:b "fake"] " case, and therefore no real letter will be posted. "
      "You can override that for testing purposes:"]
     [mui/form-control-label
      {:checked @override-fake-send?
       :onChange (fn [_ev checked] (reset! override-fake-send? checked))
       :control (r/as-element [mui/switch])
       ;:disabled (not= @review-result :send)
       :label "Post a real letter!"}]
     (when @override-fake-send?
       [mui/alert {:severity :warning}
        "Ensure there is a proper test address on the letter!"])]]
   [mui/dialog-actions]])

(defn letter-viewer []
  (let [case-id @(rf/subscribe [::case-model/case-id])
        letter-id @(rf/subscribe [::show-letter])
        letter-data @(rf/subscribe [::show-letter-data])]
    [mui/stack {:sx {:height 1}}
     [mui/stack {:direction :row}
      [mui/icon-button {:onClick #(rf/dispatch [::show-letter nil])
                        :sx {:align-self :center
                             :ml 1}}
       [ui/icon-arrow-back-sharp]]
      [mui/list-item
       {:secondaryAction (r/as-element
                           [mui/stack {:direction :row
                                       :spacing 1}
                            [mui/button {:variant :outlined
                                         :startIcon (r/as-element [ui/icon-edit])
                                         :endIcon (r/as-element [ui/icon-expand-more])
                                         :onClick #(reset! edit-anchor (ui/event-currentTarget %))}
                             "edit"]
                            [edit-popover]
                            [mui/button {:variant :contained
                                         :endIcon (r/as-element [ui/icon-send])}
                             "send"]
                            [send-confirmation-dialog]])}
       [mui/list-item-icon [ui/icon-description-outlined {:color :unset}]]
       [mui/list-item-text
        {:primary "notification letter"
         :secondary (str "generated from case data"
                      " (" (date-util/show-date-local-numeric (:modified-at letter-data)) ")")
         :sx {:flex-grow 0}}]]]
     [:iframe {:style {:height "100%"}
               :src (str "/api/case/" case-id "/notification-letter/" letter-id "/pdf")}]]))

(def completed (r/atom false))

(defn right-panel []
  [:<>
   [mui/dialog-title
    (str "household bills for " @(rf/subscribe [::issuer-label]))
    [mui/icon-button {:onClick #(rf/dispatch [::set-dialog-open nil])
                      :disabled @(rf/subscribe [::form-submitting?])}
     [ui/icon-close]]]
   [mui/dialog-content
    [mui/stack {:spacing 2}
     [mui/stack
      [mui/typography
       "For property at address"]
      [mui/stack
       [common/address-box false
        (let [address @(rf/subscribe [::property-address])]
          address)]]]
     [mui/stack
      [mui/typography {:sx {:font-weight 600}}
       "Utility accounts"]
      [mui/stack {:spacing 2}
       (for [utility @(rf/subscribe [::dialog-utilities])]
         ^{:key (:id utility)}
         [utility-row utility])]]
     (let [notification-ongoing? @(rf/subscribe [::notification-ongoing])]
       (when-not notification-ongoing?
         [mui/stack
          [mui/typography {:sx {:font-weight 600}}
           "Finished?"]
          [mui/typography
           "Let us know when you have provided all bill data at this address"
           " for company \"" @(rf/subscribe [::issuer-label]) "\". At that point,"
           " we will notify the company about the decease and ask for confirmation"
           " of the data entered."]
          [mui/form-control-label
           {:label "I have completed all bill data."
            :control
            (r/as-element
              [mui/checkbox {:checked @completed
                             :onChange #(reset! completed (ui/event-target-checked %))}])}]]))]]
   [mui/dialog-actions
    [mui/fade {:in @completed}
     [mui/button {:variant :contained
                  :onClick (let [case-id @(rf/subscribe [::case-model/case-id])
                                 context @(rf/subscribe [::dialog-context])]
                             #(rf/dispatch [::start-notification-process case-id context]))
                  :sx {:visibility (if @completed :visible :hidden)}}
      "Notify company"]]
    [mui/button {:variant :outlined
                 :onClick #(rf/dispatch [::set-dialog-open nil])}
     "Close"]]])

(defn dialog-content []
  (let [notification-ongoing? @(rf/subscribe [::notification-ongoing])
        show-letter @(rf/subscribe [::show-letter])]
    [mui/stack {:spacing 1
                :direction :row
                :sx {:height "95vh"}}
     [mui/collapse (-> {:in notification-ongoing?
                        :orientation :horizontal
                        :spacing 1
                        :sx {:flex-grow 1}}
                     (ui/make-collapse-contents-full-width))
      (if show-letter
        [letter-viewer]
        [conversation])]
     [mui/stack {:sx {:width 620}}
      [right-panel]]]))

(defn dialog-content* []
  ; Dialog content separated to its own component, for running finally when unmounted.
  (r/with-let []
    [dialog-content]
    (finally
      (js/console.log "disposing of dialog-content"))))
      ;(reset! completed false)
      ;(reset! anchor nil)
      ;(reset! edit-anchor nil)
      ;(rf/dispatch-sync [::dispose-conversation])
      ;(rf/dispatch-sync [::show-letter nil]))))

(defn dialog []
  (let [notification-ongoing? @(rf/subscribe [::notification-ongoing])]
    (js/console.log "rendering dialog")
    [mui/dialog {:open (boolean @(rf/subscribe [::dialog-open?]))
                 :maxWidth (if notification-ongoing? :xl :sm)
                 :fullWidth true}
     [dialog-content*]]))

(defn show {:arglists '({:keys [utility-company property]})}
  [dialog-context]
  (rf/dispatch [::set-dialog-open dialog-context]))
