(ns darbylaw.web.semantic
  (:require
    ["semantic-ui-react" :as s]
    [re-frame.core :as re-frame]
    [darbylaw.web.styles :as styles]
    [reagent.core :as r]))

(defn navbar []
  [:> s/Menu {:fluid "horizontal" :borderless true}
   [:> s/Menu.Item {:header true} "probate-tree"]
   [:> s/Menu.Item {:position "right"} "account"]
   ])


(defn get-started []
  [:div
   [navbar]


   [:> s/Container {:text true}

    [:h1 {:style {:font-weight 600}} "get started"]
    [:p "It looks like you need probate. Here are some quick questions about you. Then we will ask about the deceased and their relationship to you."]


    [:> s/Form
     [:> s/Form.Field

      [:> s/Form.Select {:fluid true :label "Title" :options [{:text "Mr"} {:text "Ms"}]}]
      ]
     [:> s/Grid {:columns 2 :fluid true}
      [:> s/Grid.Row
       [:> s/Grid.Column
        [:> s/Form.Input {:placeholder "Forenames"}]
        ]
       [:> s/Grid.Column
        [:> s/Form.Input {:placeholder "Surname" :style {:padding-bottom "0.5rem"}}]
        ]

       ]
      ]
     [:> s/Form.Input {:label "Date of Birth" :placeholder "DD/MM/YYY" :fluid true}]
     [:> s/Form.TextArea {:placeholder "Address" :fluid true}]
     [:> s/Form.Input {:placeholder "Postcode" :fluid true}]
     [:> s/Form.Input {:placeholder "Telephone" :fluid true}]
     [:> s/Form.Input {:placeholder "Email" :fluid true}]
     [:> s/Button {:fluid true :style {:background-color :#333333 :color :#f5f5f5}} "Next"]
     ]
    ]
   ])


