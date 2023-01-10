# >> ECR
# A lambda to send notifications to slack
# Requirements:
# - A slack oauth token with the `chat:write`


# >> Slack Token

resource "aws_ssm_parameter" "slack_token" {
  name  = "ProbateTree_SlackToken"
  type  = "SecureString"
  value = "xxx"

  lifecycle {
    ignore_changes = [value]
  }
}



# >> IAM Role

resource "aws_iam_role" "slack_lambda" {
  name = "slack_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

data "aws_iam_policy_document" "lambda_cloudwatch_policy" {
  statement {
    effect  = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = ["arn:aws:logs:*:*:*"]
  }
}

resource "aws_iam_role_policy" "lambda_cloudwatch_policy" {
  name = "lambda_cloudwatch_policy"
  role = aws_iam_role.slack_lambda.id
  policy = data.aws_iam_policy_document.lambda_cloudwatch_policy.json
}

data "aws_iam_policy_document" "lambda_ssm_policy" {
  statement {
    effect  = "Allow"
    actions = ["ssm:GetParameter"]

    resources = [aws_ssm_parameter.slack_token.arn]
  }
}

resource "aws_iam_role_policy" "lambda_ssm_policy" {
  name = "lambda_ssm_policy"
  role = aws_iam_role.slack_lambda.id
  policy = data.aws_iam_policy_document.lambda_ssm_policy.json
}



# >> Lambda

data "archive_file" "slack_lambda" {
  type        = "zip"
  source_dir  = "slack_lambda"
  output_path = "${path.module}/.build/slack_lambda.zip"
}

resource "aws_lambda_function" "slack_lambda" {
  filename      = data.archive_file.slack_lambda.output_path
  function_name = "slack_lambda"
  role          = aws_iam_role.slack_lambda.arn
  handler       = "slack_lambda.handler"

  source_code_hash = data.archive_file.slack_lambda.output_base64sha256

  runtime = "python3.8"
}
