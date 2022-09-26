# Appliction Infra

The terraform in this folder is for deploying the appliction to various *environments*.

It uses [terraform workspaces](https://www.terraform.io/language/state/workspaces) to manage deployments into different environments.
Doing this assumes that resources in this folder will be unique to an *environment*.
If there's any shared infrastruction it should go in `infra/account`.


## Initial Setup

For when there's nothing deployed in any *environment*.

### NOTE:
You **must** manually run through these steps for the first time.
This creates the ECR that the CI deploys the docker container to.
Without doing it manually the docker push would fail, a bit of a chicken and egg situation 🐣

```bash
# NOTE: You will have to make sure your shell has permissions, look into aws-vault
# NOTE: Requires the terraform state bucket to exist in the account, see `infra/account`
$ tf init
# Create workspace to deploy production resources to
$ tf workspace new production
$ tf apply -var "darbylaw_docker_tag=latest"
```


## Usual Operation

Usually you can just let the CI run, but in the case that you need to manually run things:

```bash
$ tf init
$ tf workspace select production
# Now do whatever you want :P
```
