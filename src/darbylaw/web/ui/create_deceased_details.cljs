(ns darbylaw.web.ui.create-deceased-details
  (:require [darbylaw.web.routes :as routes]
            [darbylaw.web.ui.deceased-details-form :as form]))

(defmethod routes/panels :create-deceased-details-panel []
  [form/panel :create {}])
