(ns darbylaw.web.ui.error-boundary
  (:require [reagent.core :as r]
            [reagent-mui.components :as mui]))

(defn default-error-body [_error reset]
  [mui/alert {:severity :error
              :action (r/as-element
                        [:<>
                         [mui/button {:onClick #(reset)
                                      :size :small}
                          "Retry"]
                         [mui/button {:onClick #(js/window.location.reload)
                                      :size :small}
                          "Refresh"]])}
   "Something went wrong!"])

(defn error-boundary
  ([body]
   [error-boundary
    {:error-body default-error-body}
    body])
  ([{:keys [error-body]} _body]
   (assert (fn? error-body))
   (r/create-class
     {:display-name "ErrorBoundary"
      :component-did-catch (fn [this err _info]
                             (.setState this nil) ;; This prevents dev warnings
                             (r/set-state this {:error err
                                                :error-children (r/children this)}))
      :render
      (fn [this]
        (let [[_ {:keys [error-body]} body] (r/argv this)
              {:keys [error error-children]} (r/state this)
              children (r/children this)
              reset #(r/set-state this {:error nil :error-children nil})]
          (r/as-element
             (if-not (and error (= children error-children))
                 body
                 [error-body error reset]))))})))
