(ns darbylaw.workspaces.mui-demo-cards
  (:require
    [darbylaw.web.styles :as styles]
    [darbylaw.web.mui-components :as mui]
    [reagent-mui.components :as muic]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [nubank.workspaces.core :as ws]
    [nubank.workspaces.card-types.react :as ct.react]

    [reagent-mui.icons.delete-icon :as icon]))


(defn nav []
  [mui/app-bar
   [mui/toolbar {:variant "dense" :class (styles/mui-navbar)}

    [mui/typography {:variant :h5} "probate-tree"]
    [mui/button
     "account"
     ]]])


(defonce modal-visible (r/atom false))



(defn modal []

  [mui/dialog {:class (styles/mui-dashboard) :variant "alert" :open @modal-visible :on-close #(reset! modal-visible false)}
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
   [mui/container {:max-width :s :class (styles/mui-dashboard)}
    [mui/typography {:variant :h3} "other components"]
    [mui/typography {:variant :h4 :font-style "italic"} "including subtitles"]
    [mui/typography {:variant :p} "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
   Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."]

    [mui/stack {:spacing 2 :direction :row}
     [mui/stack {:spacing 1 :sx {:min-width "50%" :align-items :flex-start :margin-top "1rem"}}
      [mui/form-control {:sx {:min-width "100%"}}

       [mui/form-label {:control (r/as-element [mui/switch]) :label "Switch"}]

       [mui/form-label {:control (r/as-element [mui/checkbox]) :label "Checkbox"}]
       [mui/form-label {:control (r/as-element [mui/checkbox {:color "secondary"}]) :label "Custom Colour"}]

       [mui/form-label {:label-placement :bottom :control (r/as-element [mui/slider {:color "primary" :size "small" :default-value 70}]) :label "Slider"}]

       ]
      ]
     [mui/stack {:spacing 1}
      [mui/typography {:variant :h4} "column layout"]
      [mui/button {:on-click #(reset! modal-visible true) :variant "contained" :sx {:color :black}} "modal"]
      [modal]
      ]
     ]]])



(ws/defcard mui-demo-card
  (ct.react/react-card
    (r/as-element [mui-tester])))