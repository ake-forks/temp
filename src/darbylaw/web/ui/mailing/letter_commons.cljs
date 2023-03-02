(ns darbylaw.web.ui.mailing.letter-commons)

(defn letter-state-caption [letter-data]
  (let [{:keys [send-action upload-state send-state send-error]} letter-data]
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