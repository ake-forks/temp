terraform {
  backend "s3" {
    bucket = "tf-state-darbylaw"
    # NOTE: All environments are stored under this key.
    #       We use `tf workspace`s to deploy to different environments.
    # NOTE: The above will only work well while all environment contain the
    #       same assets.
    key    = "app/terraform.tfstate"
    region = "eu-west-2"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 4.0.0"
    }
    random = {
      source  = "hashicorp/random"
      version = ">= 3.0.0"
    }
  }
}

provider "aws" {
  region = "eu-west-2"
}

provider "random" {
}