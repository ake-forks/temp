(ns darbylaw.web.routes)

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])
