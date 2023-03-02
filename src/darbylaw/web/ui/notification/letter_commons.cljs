(ns darbylaw.web.ui.notification.letter-commons
  (:require [darbylaw.web.ui :as ui]
            [reagent-mui.components :as mui]))

(defn letter-header [{:keys [on-back]} & children]
  (into
    [mui/stack {:direction :row}
     [mui/icon-button {:onClick on-back
                       :sx {:align-self :center
                            :ml 1}}
      [ui/icon-arrow-back-sharp]]]
    children))
