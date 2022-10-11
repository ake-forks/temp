# >> Execution Role
# This role is used by the *task* to make AWS API calls
# This is stuff like pulling a container & sending logs

resource "aws_iam_role" "execution_role" {
  name = "probatetree-exec-role-${terraform.workspace}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
        Effect = "Allow"
        Sid    = ""
      }
    ]
  })
}

# Built in TaskExecutionRolePolicy
resource "aws_iam_role_policy_attachment" "execution_role_policy_attachment" {
  role       = aws_iam_role.execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Secret Access Policy
data "aws_iam_policy_document" "secret_access" {
  statement {
    sid    = "AllowAccessDatabaseAdminPassword"
    effect = "Allow"
    actions = [
      "ssm:GetParameters"
    ]
    resources = [
      aws_ssm_parameter.xtdb-backend-admin-password.arn
    ]
  }
}

resource "aws_iam_policy" "secret_access" {
  name   = "ProbateTreeWebserverSecretAccessPolicy-${terraform.workspace}"
  path   = "/"
  policy = data.aws_iam_policy_document.secret_access.json
}

resource "aws_iam_role_policy_attachment" "execution_role_secret_access_policy_attachment" {
  role       = aws_iam_role.execution_role.name
  policy_arn = aws_iam_policy.secret_access.arn
}
