(ns darbylaw.web.routes)

(defmulti panels identity)
(defmethod panels :waiting-for-routes [] [:div])
(defmethod panels :default [] [:div "No panel found for this route."])
