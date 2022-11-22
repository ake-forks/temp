# >> VPC


# >> Import VPC

locals {
  vpc_name = "main"
}

# Created in `account`
data "aws_vpc" "main" {
  tags = {
    Name = "${local.vpc_name}"
  }
}



# >> Subnets

# Only get private IPs
# Connected to a NAT Gateway
data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }

  tags = {
    Name = "${local.vpc_name}-private-*"
  }
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }

  tags = {
    Name = "${local.vpc_name}-public-*"
  }
}
