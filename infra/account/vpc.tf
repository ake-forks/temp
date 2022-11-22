# >> VPC
#
# The aim is to enable the following architecture:
# - Only things which *must* be publicly accessable be in the public subnet
#   - E.g the Load Balancer, but not the database or app
# - Everything else in the private subnet
#
# This means that we will need:
# - Public subnets
#   - With Internet Gateway
#   - And NAT Gateway
# - Private subnets


# >> VPC

module "main-vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.18.1"

  name = "main"
  cidr = "10.0.0.0/16"

  # Setup Availability Zones and Subnets
  azs             = [for zone in ["a", "b", "c"] : "${local.region}${zone}"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  # Setup NAT Gateways so private subnets have internet access
  enable_nat_gateway = true
  single_nat_gateway = false
}
