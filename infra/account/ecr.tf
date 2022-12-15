# >> ECR
# A shared ECR registry for the probatetree docker image

resource "aws_ecr_repository" "probatetree" {
  name = "probatetree"
}
