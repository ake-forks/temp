terraform {
  backend "s3" {
    bucket = "tf-state-darbylaw"
    key    = "account/terraform.tfstate"
    region = "eu-west-2"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 4.0.0"
    }
  }
}
