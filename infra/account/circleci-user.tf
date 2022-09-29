# >> User
# This is the user that CircleCI will use to deploy infrastructure to AWS with

resource "aws_iam_user" "circleci" {
  name = "CircleCIAutomationUser"
}

# NOTE: This stores the secrets in the state file.
#       The S3 store is encrypted and private, is that enough?
resource "aws_iam_access_key" "circleci" {
  user = aws_iam_user.circleci.name
}

output "circleci_access_key_id" {
  value = aws_iam_access_key.circleci.id
}

output "circleci_secret_access_key" {
  value     = aws_iam_access_key.circleci.secret
  sensitive = true
}



# >> Policy
# This policy describes the access that CircleCI will have to the account

# Looking at `infra/app` currently it needs to deploy:
# - ECR repos
# - ECS
# - IAM
# - EC2
#   - Security Groups
#   - Load Balancers

resource "aws_iam_user_policy" "circleci" {
  name = "CircleCIAutomationPolicy"
  user = aws_iam_user.circleci.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "AllowECRAccess"
        Action   = "ecr:*"
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Sid      = "AllowECSAccess"
        Action   = "ecs:*"
        Effect   = "Allow"
        Resource = "*"
      },
      # TODO: Restrict this so that it can't edit it's own policy?
      {
        Sid      = "AllowIAMAccess"
        Action   = "iam:*"
        Effect   = "Allow"
        Resource = "*"
      },
      # For Security Groups & Load Balancers
      {
        Sid      = "AllowEC2Access"
        Action   = "ec2:*"
        Effect   = "Allow"
        Resource = "*"
      },
      {
        Sid      = "AllowElasticLoadBalancingAccess"
        Action   = "elasticloadbalancing:*"
        Effect   = "Allow"
        Resource = "*"
      },
      # For access to terraform
      # TODO: Is this restrictive enough?
      #       Should probably split into multiple statements
      {
        Sid    = "AllowTerraformStateAccess"
        Action = "s3:*"
        Effect = "Allow"
        Resource = [
          "arn:aws:s3:::${aws_s3_bucket.tf-state-bucket.bucket}",
          "arn:aws:s3:::${aws_s3_bucket.tf-state-bucket.bucket}/env:/*/app/*"
        ]
      }
    ]
  })
}
