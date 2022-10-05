(ns darbylaw.workspaces.mui-cards
  (:require [reagent.core :as reagent]
            [darbylaw.web.mui-components :as mui]
            [darbylaw.web.mui.stepper-components :as stepper]
            [darbylaw.workspaces.workspace-styles :as style]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [darbylaw.workspaces.workspace-icons :as icon]
            ))

(defn nav []
  [mui/app-bar {}
   [mui/toolbar {:variant "dense" :class (style/mui-navbar)}

    [mui/typography {:variant :h5} "probate-tree"]
    [mui/button
     "account"
     ]]])


;MUI get started form

(defn form [title]
  [mui/stack {:spacing 1}
   [mui/typography {:variant :h5} title]
   [mui/form-control
    [mui/input-label "Title"]
    [mui/select {:placeholder "Mr"}
     [mui/menu-item {:value "Mr"} "Mr"]
     [mui/menu-item {:value "Ms"} "Ms"]
     [mui/menu-item {:value "Mx"} "Mx"]
     [mui/menu-item {:value "Dr"} "Dr"]
     [mui/menu-item {:value "Other"} "Other"]
     ]
    ]

   [mui/stack {:direction "row" :spacing 1}
    [mui/text-field {:label "Forenames" :variant "filled" :full-width true}]
    [mui/text-field {:label "Surname" :variant "filled" :full-width true}]
    ]
   [mui/input-label "Date of Birth"]
   [mui/text-field {:label "DD/MM/YY" :variant "filled"}]
   [mui/text-field {:label "Address" :variant "filled" :multiline true :min-rows 3 :max-rows 5}]
   [mui/text-field {:label "Postcode" :variant "filled"}]
   [mui/input-label "Contact Details"]
   [mui/text-field {:label "Telephone" :variant "filled"}]
   [mui/text-field {:label "Email" :variant "filled"}]
   [mui/button {:full-width true} "next"]
   ])


(defn get-started []
  [mui/container {:max-width :sm :class (style/mui-dashboard)}
   [mui/stack {:spacing 2}
    [mui/typography {:variant :h3} "get started"]
    [mui/typography {:variant :p}
     "It looks like you need probate. Here are some quick questions about you. Then we will ask about the deceased and their relationship to you."]
    [form "your details"]
    ]
   ]
  )


;MUI institution card



(defn asset-item [name amount]
  [mui/stack {:sx {:margin-top "0.3rem" :margin-bottom "0.2rem"} :direction :row :spacing 0.5 :justify-content :space-between :align-items :center}
   [icon/mui-account-balance]
   [mui/typography {:variant :h5} name]
   [mui/typography {:variant :h5} "£" amount]
   ])

(defn add-account []

  [mui/stack {:sx {:padding "0.2rem"} :direction :row :spacing 0.5 :justify-content :flex-start :align-items :center}

   [mui/typography {:variant :h5} "add account"]
   [icon/mui-add]
   ])

(defn asset-card []
  [mui/card {:className (style/mui-default)}
   [mui/card-content
    [mui/typography {:variant :h5 :sx {:font-weight :bold}} "bank accounts"]
    [mui/divider {:variant :middle}]
    [asset-item "Barclays" 5000]
    [mui/divider {:variant :middle}]
    [asset-item "Santander" 3500]
    [mui/divider {:variant :middle}]
    ]
   [mui/card-action-area
    [add-account]]
   ]
  )


;MUI progress bar

(def green [:img {:src "/images/green.png" :width 30 :height 30}])
(def orange [:img {:src "/images/orange.png" :width 30 :height 30}])

(defn progress-bar []
  [stepper/stepper {:alternativeLabel true :className (style/mui-default)}
   [stepper/step
    [stepper/label {:icon (reagent/as-element green)} "Upload Will"]
    ]
   [stepper/step
    [stepper/label {:icon (reagent/as-element green)} "Add Banks"]
    ]
   [stepper/step
    [stepper/label {:icon (reagent/as-element orange)} "Notify Institutions"]
    ]
   [stepper/step
    [stepper/label {:icon (reagent/as-element orange)} "Close Utility Accounts"]
    ]
   ]
  )



;MUI demo page

(defonce modal-visible (reagent/atom false))



(defn modal []

  [mui/dialog {:class (style/mui-dashboard) :variant "alert" :open @modal-visible :on-close #(reset! modal-visible false)}
   [mui/typography {:variant :h5} "proceed?"]
   [mui/divider]
   [mui/typography {:variant :p} "You haven't started the ID check yet.
                                    Would you like to proceed to your dashboard?
                                    You can always start the ID lookup later."]
   [mui/stack {:direction :row :justify-content :space-evenly}
    [mui/button {:variant :contained} "Proceed to Dashboard"]
    [mui/button {:variant :contained} "Cancel"]
    ]])



(defn mui-tester []
  [:div
   [nav]
   [mui/container {:max-width :s :class (style/mui-dashboard)}

    [mui/typography {:variant :h3} "other components"]
    [mui/typography {:variant :h4 :font-style "italic"} "including subtitles"]
    [mui/typography {:variant :p} "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
   Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."]

    [mui/stack {:spacing 2 :direction :row}
     [mui/stack {:spacing 1 :sx {:min-width "50%" :align-items :flex-start :margin-top "1rem"}}
      [mui/form-control {:sx {:min-width "100%"}}

       [mui/form-label {:control (reagent/as-element [mui/switch]) :label "Switch"}]

       [mui/form-label {:control (reagent/as-element [mui/checkbox]) :label "Checkbox"}]
       [mui/form-label {:control (reagent/as-element [mui/checkbox {:color "secondary"}]) :label "Custom Colour"}]

       [mui/form-label {:label-placement :bottom :control (reagent/as-element [mui/slider {:color "primary" :size "small" :default-value 70}]) :label "Slider"}]

       [mui/stack {:spacing 2 :direction :row :sx {:margin-top "2rem"}}
        [mui/button {:variant :contained} "Contained"]
        [mui/button {:variant :outlined :style {:background-color :#ffffff :color :#333333 :border "1px solid #333333"}} "Outlined"]
        ]

       ]
      ]
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h4} "column layout"]
      [mui/button {:on-click #(reset! modal-visible true) :variant "contained" :sx {:color :black}} "modal"]
      [modal]
      ]
     ]]])



;Creating workplace cards for display ----------------------------
(ws/defcard bank-card
  (ct.react/react-card
    (reagent/as-element [asset-card])))

(ws/defcard get-started-form
  (ct.react/react-card
    (reagent/as-element [get-started])))

(ws/defcard progress-element
  (ct.react/react-card
    (reagent/as-element [progress-bar])))

(ws/defcard demo-page
  (ct.react/react-card
    (reagent/as-element [mui-tester])))