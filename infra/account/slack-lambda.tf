# >> Slack Lambda
# Global config required for the slack lambda found in `infra/app/slack-lambda.tf`


# >> Slack Token

resource "aws_ssm_parameter" "slack_token" {
  name  = "ProbateTree_SlackToken"
  type  = "SecureString"
  value = "xxx"

  lifecycle {
    ignore_changes = [value]
  }
}
