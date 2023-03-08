(ns darbylaw.web.ui.mailing.letter-commons
  (:require [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]))

(defn letter-state-caption [letter-data]
  (let [{:keys [upload-state send-state send-error]
         :mail/keys [send-action]} letter-data]
    (cond
      (some? send-state)
      (str
        (case send-state
          :error "send error"
          (name send-state))
        (when (= send-state :error)
          (str " (" send-error ")"))
        (when (= send-action :fake-send)
          " [fake]"))

      (some? upload-state)
      (str (name upload-state)
           (when (= send-action :fake-send)
             " [fake]"))

      (some? send-action)
      (case send-action
        :send "sending"
        :fake-send "sending [fake]"
        :do-not-send "not to be sent")

      :else
      "in preparation")))

(defn icon-letter-arrow [direction]
  ; Icon with badge containing arrow
  [mui/badge {:badgeContent (case direction
                              :incoming "\u2199"
                              :outgoing "\u2197")
              ;:anchorOrigin {:vertical :bottom
              ;               :horizontal :right}
              #_#_:color :primary
              :sx {"& .MuiBadge-badge" {:top 4
                                        :right -4
                                        :font-weight :bold
                                        #_#_:background-color :text.primary}}}
   [ui/icon-mail-outlined]]
  #_[mui/stack {:direction :row}
     [ui/icon-mail-outlined]
     "\u2199"])

(defn icon-received-letter []
  (icon-letter-arrow :incoming))

(defn icon-outgoing-letter []
  (icon-letter-arrow :outgoing))

(defn icon-letter-draft []
  [ui/icon-drafts-outlined])

(defn get-icon-outgoing-letter [letter-data]
  (let [{:mail/keys [send-action]} letter-data]
    (cond
      (some? send-action)
      [icon-outgoing-letter]

      :else
      [icon-letter-draft])))