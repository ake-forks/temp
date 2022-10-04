(ns darbylaw.web.antd
  (:require
    [darbylaw.web.styles :as styles]
    ["antd" :as antd]
    [darbylaw.web.events :as events]
    [re-frame.core :as re-frame]
    [reagent.core :as r]))


(defn navbar []
  [:div
   [:> antd/PageHeader {:title "probate-tree"
                        :extra (r/as-element [:> antd/Button {:type "text"} "account"])
                        }
    ]
   ]
  )

(defn form []
  [:h5 "your details"]
  [:> antd/Form {:layout :vertical}
   [:> antd/Form.Item {:label "Title"}
    [:> antd/Select {:default-value "Mr"}
     [:p "Mr"]
     [:p "Ms"]
     [:p "Mx"]
     [:p "Other"]
     ;for some reason Option components aren't working
     ;[:> antd/Option "Mr"]
     ;[:> antd/Option "Ms"]
     ]
    ]
   [:> antd/Form.Item
    [:> antd/Row {:gutter 10}
     [:> antd/Col {:span 12}
      [:> antd/Input {:placeholder "Forenames"}]]
     [:> antd/Col {:span 12}
      [:> antd/Input {:placeholder "Surname"}]]
     ]
    ]
   [:> antd/Form.Item {:label "Date of Birth"}
    [:> antd/Input {:placeholder "DD/MM/YYYY"}]
    ]

   [:> antd/Form.Item [:> antd/Input.TextArea {:rows 3 :placeholder "Address"}]]
   [:> antd/Form.Item [:> antd/Input {:placeholder "Postcode"}]]
   [:> antd/Form.Item [:> antd/Input {:placeholder "Telephone"}]]
   [:> antd/Form.Item [:> antd/Input {:placeholder "Email"}]]
   [:> antd/Button {:block true :style {:background-color :#000000 :color :#ffffff} :on-click #(re-frame/dispatch [::events/navigate :antd-example])} "Next"]

   ]
  )

(defn get-started []

  [:> antd/Row
   [:> antd/Col {:span 6}]
   [:> antd/Col {:span 12}

    [:h1 {:style {:font-weight 600}} "get started"]
    [:p "It looks like you need probate. Here are some quick questions about you. Then we will ask about the deceased and their relationship to you."]
    [form]
    ]
   [:> antd/Col {:span 6}]
   ]

  )


