# Appliction Infra

The terraform in this folder is for deploying the appliction to various *environments*.

It uses [terraform workspaces](https://www.terraform.io/language/state/workspaces) to manage deployments into different environments.
Doing this assumes that resources in this folder will be unique to an *environment*.
If there's any shared infrastruction it should go in `infra/account`.



## Initial Setup

For when there's nothing deployed in any *environment*.

Deploy the `infra/account` terraform to create resources this infra depends on.



## Usual Operation

Usually you can just let the CI run, but in the case that you need to manually run things:

```bash
$ tf init
$ tf workspace select production
# Now do whatever you want :P
```
