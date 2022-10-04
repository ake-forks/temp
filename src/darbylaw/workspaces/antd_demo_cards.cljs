(ns darbylaw.workspaces.antd-demo-cards
  (:require
    [darbylaw.web.styles :as styles]
    [darbylaw.workspaces.workspace-styles :as workstyles]
    ["antd" :as antd]
    [darbylaw.web.events :as events]
    [re-frame.core :as re-frame]
    [reagent.core :as r]
    [nubank.workspaces.core :as ws]
    [nubank.workspaces.card-types.react :as ct.react]
    ))

(defn navbar []
  [:div {:class (styles/antd-dashboard)}
   [:> antd/PageHeader {:title "probate-tree" :ghost false
                        :extra (r/as-element [:> antd/Button {:type "text"} "account"])
                        }
    ]
   ]
  )

(defonce modal-visible (r/atom false))

(defn antd-modal []
  [:> antd/Modal {:onOk #(reset! modal-visible false)
                  :onCancel #(reset! modal-visible false)
                  :open @modal-visible :title "proceed?"
                  }
   [:p "You haven't started the ID check yet.
       Would you like to proceed to your dashboard?
       You can always start the ID lookup later."]])


(defn antd-tester []


  [:> antd/Layout
   [:> antd/Layout.Content
    [navbar]]

   [:> antd/Layout.Content {:class (styles/antd-dashboard) :style {:padding "2rem"}}
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
  )


(ws/defcard antd-demo-card
  (ct.react/react-card
    (r/as-element [antd-tester])))