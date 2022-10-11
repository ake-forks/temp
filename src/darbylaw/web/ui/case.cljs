(ns darbylaw.web.ui.case
  (:require [reagent-mui.components :as mui]
            [darbylaw.web.routes :as routes]
            [re-frame.core :as rf]))

(defn panel []
  (let [case-id (-> @(rf/subscribe [::routes/route-params])
                  :case-id)]
    [mui/typography {:variant :h3}
     (str "case " case-id)]))

(defmethod routes/panels :case-panel [] [panel])
