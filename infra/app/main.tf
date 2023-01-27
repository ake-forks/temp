locals {
  environments = {
    production = {
      hosted_zone_id   = "Z0021879STRZ8VWEL8XM"
      hosted_zone_name = "probatetree.com"
      subdomain        = "www"
      desired_count    = 2
    }
    staging = {
      hosted_zone_id   = "Z0021879STRZ8VWEL8XM"
      hosted_zone_name = "probatetree.com"
      subdomain        = "staging"
      desired_count    = 1
    }
  }
  config = lookup(local.environments, terraform.workspace)
}


# >> ECR

data "aws_ecr_repository" "probatetree" {
  name = "probatetree"
}

resource "aws_ecs_cluster" "probatetree" {
  name = "probatetree-${terraform.workspace}"
}



# >> Secrets

resource "aws_ssm_parameter" "auth-map" {
  name  = "ProbateTree_AuthMap_${terraform.workspace}"
  type  = "SecureString"
  value = "{}"

  lifecycle {
    ignore_changes = [value]
  }
}

resource "aws_ssm_parameter" "post-service-map" {
  name  = "ProbateTree_PostService_${terraform.workspace}"
  type  = "SecureString"
  value = "{}"

  lifecycle {
    ignore_changes = [value]
  }
}



# >> Logs

resource "aws_cloudwatch_log_group" "probatetree-logs" {
  name              = "/fargate/service/probatetree-${terraform.workspace}"
  retention_in_days = 30
}

resource "aws_lambda_permission" "webapp_logging" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_lambda.function_name
  principal     = "logs.${local.region}.amazonaws.com"
  source_arn    = "${aws_cloudwatch_log_group.probatetree-logs.arn}:*"
}

resource "aws_cloudwatch_log_subscription_filter" "webapp_logging" {
  depends_on      = [aws_lambda_permission.webapp_logging]
  destination_arn = aws_lambda_function.slack_lambda.arn
  filter_pattern  = ""
  log_group_name  = aws_cloudwatch_log_group.probatetree-logs.name
  name            = "webapp_to_slack-${terraform.workspace}"
}



# >> Task Definition

locals {
  # The port the container exposes
  container_port = 8080
}

resource "aws_ecs_task_definition" "probatetree" {
  family                   = "probatetree-${terraform.workspace}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu                = 1024
  memory             = 2048
  execution_role_arn = aws_iam_role.execution_role.arn
  task_role_arn      = aws_iam_role.task_role.arn

  container_definitions = jsonencode(
    [
      {
        name      = "webserver"
        image     = "${data.aws_ecr_repository.probatetree.repository_url}:${var.probatetree_docker_tag}"
        essential = true
        portMappings = [
          {
            protocol      = "tcp"
            containerPort = local.container_port
            hostPort      = local.container_port
          }
        ]
        # TODO: Don't give the application admin access to the database?
        environment = [
          {
            "name" : "PROFILE"
            "value" : terraform.workspace
          },
          {
            "name" : "DOC_STORE_BUCKET"
            "value" : aws_s3_bucket.doc-store.bucket
          },
          {
            "name" : "DATABASE_HOST"
            "value" : aws_db_instance.xtdb-backend.address
          },
          {
            "name" : "DATABASE_PORT"
            "value" : tostring(aws_db_instance.xtdb-backend.port)
          },
          {
            "name" : "DATABASE_DBNAME"
            "value" : aws_db_instance.xtdb-backend.db_name
          },
          {
            "name" : "DATABASE_USER"
            "value" : local.xtdb-backend-admin-username
          }
        ]
        secrets = [
          {
            "name" : "DATABASE_PASSWORD"
            "valueFrom" : aws_ssm_parameter.xtdb-backend-admin-password.arn
          },
          {
            "name" : "AUTH_MAP"
            "valueFrom" : aws_ssm_parameter.auth-map.arn
          },
          {
            "name" : "POST_SERVICE_MAP"
            "valueFrom" : aws_ssm_parameter.post-service-map.arn
          }
        ]
        logConfiguration = {
          logDriver = "awslogs"
          options = {
            "awslogs-group"         = aws_cloudwatch_log_group.probatetree-logs.name
            "awslogs-region"        = "eu-west-2"
            "awslogs-stream-prefix" = "ecs"
          }
        }
      }
    ]
  )
}



# >> Security Groups

resource "aws_security_group" "task" {
  name   = "probatetree-task-sg-${terraform.workspace}"
  vpc_id = data.aws_vpc.main.id

  ingress {
    protocol         = "tcp"
    from_port        = local.container_port
    to_port          = local.container_port
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    protocol         = "-1"
    from_port        = 0
    to_port          = 0
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_security_group" "lb" {
  name   = "probatetree-lb-sg-${terraform.workspace}"
  vpc_id = data.aws_vpc.main.id

  ingress {
    protocol         = "tcp"
    from_port        = 80
    to_port          = 80
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    protocol         = "tcp"
    from_port        = 443
    to_port          = 443
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    protocol         = "-1"
    from_port        = 0
    to_port          = 0
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}



# >> Load Balancer

resource "aws_lb" "ingress" {
  name               = "probatetree-lb-${terraform.workspace}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb.id]
  subnets            = data.aws_subnets.public.ids

  enable_deletion_protection = true
}

resource "aws_lb_target_group" "probatetree" {
  name        = "probatetree-tg-${terraform.workspace}"
  port        = local.container_port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.main.id
  target_type = "ip"

  health_check {
    healthy_threshold   = "2"
    unhealthy_threshold = "2"
    interval            = "10"
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = "3"
    path                = "/healthcheck"
  }
}

# Redirect all HTTP traffic to use HTTPS
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.ingress.id
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = 443
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# Forward HTTPS traffic onto the container
# Does TSL termination
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.ingress.id
  port              = 443
  protocol          = "HTTPS"

  ssl_policy      = "ELBSecurityPolicy-2016-08"
  certificate_arn = aws_acm_certificate.probatetree.arn

  default_action {
    type = "forward"

    target_group_arn = aws_lb_target_group.probatetree.id
  }
}



# >> ECS Service

resource "aws_ecs_service" "probatetree" {
  name            = "probatetree-${terraform.workspace}"
  cluster         = aws_ecs_cluster.probatetree.id
  task_definition = aws_ecs_task_definition.probatetree.arn
  launch_type     = "FARGATE"

  desired_count = local.config.desired_count

  # NOTE: Below is an explanation of how the grace period works
  #
  # Basically, it just tells the service to *not* stop a task based on the
  # status of the healthcheck while the grace period is in effect.
  # But the *target group* and *load balancer* will still function like normal.
  # I.e. healthy tasks will still have traffic routed to them, even if the
  # grace period is in effect.
  #
  # To explain with an example, say we have:
  # - A service with one task that:
  #   - Fails the healthcheck for the first 60 seconds
  #   - Starts succeeding after those 60 seconds
  # - The service has a 120 second health check grace period set
  # - The service is attached to a load balancer via a target group
  #
  # 1. The service starts up a new task
  #   - The task starts failing health checks so:
  #     - The *target group* marks it as "unhealthy"
  #     - The *load balancer* does not route traffic to it
  #   - But because we are still within the grace period:
  #     - The *service* keeps the task as "RUNNING"
  # 2. 60 seconds pass
  #   - The task starts succeeding health checks so:
  #     - The *target group* marks it as "healthy"
  #   - Even though we are still within the *service's* grace period:
  #     - The *load balancer* starts routing traffic to it
  #   - If (for example) the task starts failing again:
  #     - The *target group* will mark it as "unhealthy"
  #     - And the *load balancer* will stop routing traffic to it
  #     - But the *service* will keep the task as "RUNNING" as we are still
  #       within the grace period
  # 3. 120 seconds pass
  #   - If the task keeps succeeding the health checks, then nothing will change
  #   - But, if the task starts failing the health checks then:
  #     - The *target group* will mark it as "unhealthy"
  #     - The *load balancer* will stop routing traffic to it
  #     - And the *service* will stop the task
  health_check_grace_period_seconds = 120

  network_configuration {
    security_groups  = [aws_security_group.task.id]
    subnets          = data.aws_subnets.private.ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.probatetree.id
    container_name   = "webserver"
    container_port   = local.container_port
  }

  # If we add autoscaling:
  # lifecycle {
  #   ignore_changes = [desired_count]
  # }
}

resource "aws_cloudwatch_event_rule" "task_state_change" {
  name        = "probatetree-task-state-change-${terraform.workspace}"
  description = "Capture each ProbateTree task state change"

  event_pattern = <<EOF
{
  "source": ["aws.ecs"],
  "detail-type": ["ECS Task State Change"],
  "detail": {
    "clusterArn": ["${aws_ecs_cluster.probatetree.arn}"]
  }
}
EOF
}

resource "aws_cloudwatch_event_target" "example" {
  arn  = aws_lambda_function.slack_lambda.arn
  rule = aws_cloudwatch_event_rule.task_state_change.id
}

resource "aws_lambda_permission" "task_state_change" {
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.slack_lambda.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.task_state_change.arn
}
