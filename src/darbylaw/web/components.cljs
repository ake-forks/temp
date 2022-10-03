(ns darbylaw.web.components
  (:require
    [darbylaw.web.styles :as styles]
    [darbylaw.web.mui-components :as mui]
    [darbylaw.web.events :as events]
    [re-frame.core :as re-frame]))

(defn navbar []
  [:div {:class (styles/navbar)}
   [:h4 "probate tree"]
   [:h4 "account"]])



;(defn card [name institutions]
;  [:div {:class (styles/card)}
;   [:h3 name]
;
;    (for [inst (keys institutions)]
;     [:div {:class "card-entry"}
;      [:h4 inst " " (get institutions inst)]
;      [:button "See More"]])
;   ])

(defn timeline [events]
  [:div {:class (styles/timeline)}
   [:h2 "your grandmother's estate"]
   [mui/button "mui"]
   [:div {:class "line"}
    (for [event (keys events)]
      [:div
       [:h4 event]
       (if (true? (get events event)) [:h1 "●"] [:h1 "◐"])
       ])]
   ])




;;MUI



(defn nav []
  [mui/app-bar {}
   [mui/toolbar {:variant "dense" :class (styles/mui-navbar)}

    [mui/typography {:variant :h5} "probate-tree"]
    [mui/button
     "account"
     ]]])

(defn card2 [name items]
  [mui/card {:raised true}
   [mui/typography {:variant :h5} name]
   (for [item items]
     [mui/typography {:variant :p} item])
   ])




;TODO mui starts here


(defn form [title]
  [mui/stack {:spacing 1}
   [mui/typography {:variant :h5} title]
   [mui/form-control
    [mui/input-label "Title"]
    [mui/select {:placeholder ""}
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
   [mui/button {:full-width true :on-click #(re-frame/dispatch [::events/navigate :mui-components])} "next"]
   ])

(defn get-started []
  [mui/container {:max-width :sm :class (styles/mui-dashboard)}
   [mui/stack {:spacing 2}
    [mui/typography {:variant :h3} "get started"]
    [mui/typography {:variant :p}
     "It looks like you need probate. Here are some quick questions about you. Then we will ask about the deceased and their relationship to you."]
    [form "your details"]
    ]
   ]
  )
(def open (atom false))
(defn close [] (swap! open false))
(defn open-dialog [] (swap! open true))

(defn mui-tester []
  [mui/container {:max-width :m :class (styles/mui-dashboard)}
   [mui/typography {:variant :h3} "other components"]
   [mui/typography {:variant :h4 :font-style "italic"} "including subtitles"]
   [mui/typography {:variant :p} "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
   Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."]

   [mui/stack {:spacing 2 :direction :row}
    [mui/stack {:spacing 1 :sx {:min-width "50%" :align-items :flex-start :margin-top "1rem"}}
     [mui/form-control {:sx {:min-width "100%"}}

      [mui/switch {:label "switch"}]
      [mui/checkbox {:label "checkbox"}]
      [mui/slider {:size "small" :default-value 70}]
      ]
     ]
    [mui/stack {:spacing 1 :sx {:align-items :center :margin-top "1rem"}}
     [mui/typography {:variant :h4} "column layout"]
     [mui/button {:on-click (fn [] open-dialog) :variant "outlined" :sx {:color :black}} "modal"]
     [mui/dialog {:variant "alert" :open (@open) :onClose (fn [] close)}
      [mui/typography {:variant :h5} "proceed?"]
      [mui/typography {:variant :p} "You haven't started the ID check yet.
                                    Would you like to proceed to your dashboard?
                                    You can always start the ID lookup later"]]
     ]
    ]])