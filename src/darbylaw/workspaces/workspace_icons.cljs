(ns darbylaw.workspaces.workspace-icons
  (:require
    [reagent-mui.icons.account-balance]
    [reagent-mui.icons.add-circle]
    [reagent-mui.icons.edit]
    [reagent-mui.icons.delete-icon]
    ["@ant-design/icons" :as antd-icon]))


(def mui-account-balance reagent-mui.icons.account-balance/account-balance)
(def mui-add reagent-mui.icons.add-circle/add-circle)
(def mui-edit reagent-mui.icons.edit/edit)
(def mui-delete reagent-mui.icons.delete-icon/delete)
(def antd-add antd-icon/PlusCircleFilled)
(def antd-bank antd-icon/BankOutlined)


(def santander [:img {:src "/images/SAN.svg"}])

(def barclays [:img {:src "/images/BCS.svg"}])