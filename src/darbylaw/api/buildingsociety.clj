(ns darbylaw.api.buildingsociety
  (:require [xtdb.api :as xt]
            [reitit.coercion]
            [reitit.coercion.malli]))



(defn routes []
  ["/bank"
   ["/:case-id"
    ["/add-buildsoc-accounts"
     {:post {:handler add-buildsoc-accounts
             :coercion reitit.coercion.malli/coercion
             :parameters {:body
                          [:map
                           [:bank-id :keyword]
                           [:accounts
                            [:vector
                             [:map
                              [:sort-code :string]
                              [:account-number :string]
                              [:estimated-value {:optional true} :string]
                              [:joint-check {:optional true} :boolean]
                              [:joint-info {:optional true} :string]]]]]}}}]
    ["/update-buildsoc-accounts"
     {:post {:handler update-buildsoc-accounts
             :coercion reitit.coercion.malli/coercion
             :parameters {:body
                          [:map
                           [:bank-id :keyword]
                           [:accounts
                            [:vector
                             [:map
                              [:sort-code :string]
                              [:account-number :string]
                              [:estimated-value {:optional true} :string]
                              [:joint-check {:optional true} :boolean]
                              [:joint-info {:optional true} :string]
                              [:confirmed-value {:optional true} :string]]]]]}}}]]])
