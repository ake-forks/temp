(ns darbylaw.workspaces.antd-cards
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [darbylaw.workspaces.workspace-styles :as style]
            ["antd" :as antd]
            [darbylaw.workspaces.workspace-icons :as icon]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]))


(defn navbar []
  [:div {:class (style/antd-dashboard)}
   [:> antd/PageHeader {:title "probate-tree" :ghost false
                        :extra (reagent/as-element [:> antd/Button {:type "text"} "account"])
                        }
    ]
   ]
  )

;Get started form

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
   [:> antd/Button {:block true :style {:background-color :#000000 :color :#ffffff}} "Next"]

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




;Institution card

(defn asset-item [name amount]
  [:> antd/Space {:direction :horizontal}
   [:> icon/antd-bank {:style {:font-size :medium}}]
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} name]
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} "£" amount]
   ]
  )

(defn add-account []
  [:> antd/Space {:direction :horizontal}
   [:> antd/Button {:type :text}
    [:h5 {:style {:font-family "'Lexend', sans-serif" :font-size :medium :margin-bottom 0}} "add account"]

    ]
   [:> icon/antd-add]

   ]
  )

(defn antd-card []

  [:> antd/Card
   {
    :bodyStyle {:padding "0.5rem" :font-weight 600}}
   [:h5 {:style {:font-family "'Lexend', sans-serif" :font-weight 600 :font-size :medium :margin-bottom 0}} "bank accounts"]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [asset-item "Santander" 5000]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [asset-item "HSBC" 3500]
   [:> antd/Divider {:style {:margin "0.5rem"}}]
   [add-account]
   ]
  )


;Progress bar

(def green [:img {:src "/images/green.png" :width 30 :height 30}])
(def orange [:img {:src "/images/orange.png" :width 30 :height 30}])


(defn timeline []
  [:> antd/Steps {:labelPlacement :vertical}
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "finish" :title "Upload Will" :icon (reagent/as-element green)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "finish" :title "Add Banks" :icon (reagent/as-element green)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "process" :title "Notify Institutions" :icon (reagent/as-element orange)}]
   [:> antd/Steps.Step {:style {:font-family "'Lexend', sans-serif"} :status "wait" :title "Close Utility Accounts" :icon (reagent/as-element orange)}]
   ]
  )



;Demo page

(defonce modal-visible (reagent/atom false))

(defn antd-modal []
  [:> antd/Modal {:onOk #(reset! modal-visible false)
                  :onCancel #(reset! modal-visible false)
                  :open @modal-visible :title "proceed?"
                  }
   [:p "You haven't started the ID check yet.
       Would you like to proceed to your dashboard?
       You can always start the ID lookup later."]])



(defn antd-tester []
  [:div
   [navbar]
   [:> antd/Layout

    [:> antd/Layout.Content {:class (style/antd-dashboard) :style {:padding "2rem"}}
     [:h1 "other components"]
     [:h1 {:style {:font-style "italic"}} "including subtitles"]
     [:p "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
   Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."]

     [:> antd/Row
      [:> antd/Col {:span 12}
       [:p [:> antd/Switch] " Switch"]
       [:p [:> antd/Checkbox] " Checkbox"]

       [:p [:> antd/Slider {:defaultValue 70}] " Slider"]
       [:> antd/Row
        [:> antd/Button {:type :primary} "Primary"]
        [:> antd/Button {:type :default} "Default"]
        [:> antd/Button {:type :text} "Text"]
        [:> antd/Button {:type :link} "Link"]
        ]
       ]
      [:> antd/Col {:span 12}
       [:h3 "Column Layout"]
       [:> antd/Button {:onClick #(reset! modal-visible true)} "modal"]
       [antd-modal]
       ]
      ]
     ]
    ]
   ]
  )


;; -- Entry Point -------------------------------------------------------------

(ws/defcard get-started-form
  (ct.react/react-card
    (reagent/as-element [get-started])))

(ws/defcard bank-card
  (ct.react/react-card
    (reagent/as-element [antd-card])))

(ws/defcard progress-element
  (ct.react/react-card
    (reagent/as-element [timeline])))

(ws/defcard demo-page
  (ct.react/react-card
    (reagent/as-element [antd-tester])))