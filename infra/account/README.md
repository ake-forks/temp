# Account Infra

The terraform in this folder is for deploying the account level infrastructure.

This is stuff that would be used regardless of what environment you're deploying to, for example the terraform state bucket.



## Inital Setup

For when there's nothing deployed in the account.

### Terraform State Bucket

First you must manually create the terraform state bucket.
This repo calls that `tf-state-darbylaw`, feel free to change that in `infra/account/s3-tf-state-bucket.tf` and the `config.tf` files.

Next you run:

```bash
# NOTE: Ensure your shell has the right permissions setup, look into aws-vault
$ tf init
```

Finally you must import all the resources in `infra/account/s3-tf-state-bucket.tf`.
Luckily this the format for each import command is roughly the same:

```bash
$ tf import "aws_s3_bucket.tf-state-bucket" "tf-state-darbylaw"
$ tf import "aws_s3_bucket_server_side_encryption_configuration.tf-state-bucket" "tf-state-darbylaw"
... etc
```



## Usual Operaion

I'm not sure we should let CI manage these resources, so for now usual operation is to manually apply them:

```bash
# NOTE: You have to make sure your shell has permissions, look into aws-vault
$ tf init
# Make whatever changes you need to
$ tf apply
```
