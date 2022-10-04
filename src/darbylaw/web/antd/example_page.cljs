(ns darbylaw.web.antd.example-page
  (:require
    [darbylaw.web.styles :as styles]
    ["antd" :as antd]
    [darbylaw.web.events :as events]
    [re-frame.core :as re-frame]
    [reagent.core :as r]))

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

   [:> antd/Layout.Content {:style {:padding "5rem"}}
    [:h1 "other components"]
    [:h1 {:style {:font-style "italic"}} "including subtitles"]
    [:p "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo.
   Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt."]

    [:> antd/Row
     [:> antd/Col {:span 12}
      [:p [:> antd/Switch] " Switch"]
      [:p [:> antd/Checkbox] " Checkbox"]
      [:p [:> antd/Checkbox {:className (styles/antd-checkbox)}] " Custom Colour"]
      [:p [:> antd/Slider {:defaultValue 70}] " Slider"]
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
