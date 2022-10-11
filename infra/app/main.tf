locals {
  environments = {
    production = {
      hosted_zone_id   = "Z0021879STRZ8VWEL8XM"
      hosted_zone_name = "probatetree.com"
      subdomain        = "www"
    }
  }
  config = lookup(local.environments, terraform.workspace)
}

# >> ECR

resource "aws_ecr_repository" "probatetree" {
  name = "probatetree-${terraform.workspace}"
}

resource "aws_ecs_cluster" "probatetree" {
  name = "probatetree-${terraform.workspace}"
}



# >> Logs

resource "aws_cloudwatch_log_group" "probatetree-logs" {
  name              = "/fargate/service/probatetree-${terraform.workspace}"
  retention_in_days = 30
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

  container_definitions = jsonencode(
    [
      {
        name      = "webserver"
        image     = "${aws_ecr_repository.probatetree.repository_url}:${var.probatetree_docker_tag}"
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
            "name" : "DATABASE_URL"
            "value" : aws_db_instance.xtdb-backend.address
          },
          {
            "name" : "DATABASE_DB"
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



# >> VPC Info

data "aws_vpc" "default_vpc" {
  default = true
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default_vpc.id]
  }
}



# >> Security Groups

resource "aws_security_group" "task" {
  name   = "probatetree-task-sg-${terraform.workspace}"
  vpc_id = data.aws_vpc.default_vpc.id

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
  vpc_id = data.aws_vpc.default_vpc.id

  ingress {
    protocol         = "tcp"
    from_port        = 80
    to_port          = 80
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  # TODO: Remove?
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
  vpc_id      = data.aws_vpc.default_vpc.id
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

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.ingress.id
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.probatetree.id
  }
}

resource "aws_acm_certificate" "probatetree" {
  domain_name       = "${local.config.subdomain}.${local.config.hosted_zone_name}"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "probatetree" {
  zone_id = local.config.hosted_zone_id
  name    = "${local.config.subdomain}.${local.config.hosted_zone_name}"
  type    = "A"

  alias {
    name                   = aws_lb.ingress.dns_name
    zone_id                = aws_lb.ingress.zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "probatetree_cert_validation" {
  allow_overwrite = true
  name            = tolist(aws_acm_certificate.probatetree.domain_validation_options)[0].resource_record_name
  records         = [tolist(aws_acm_certificate.probatetree.domain_validation_options)[0].resource_record_value]
  type            = tolist(aws_acm_certificate.probatetree.domain_validation_options)[0].resource_record_type
  zone_id         = local.config.hosted_zone_id
  ttl             = 60
}

resource "aws_acm_certificate_validation" "probatetree" {
  certificate_arn         = aws_acm_certificate.probatetree.arn
  validation_record_fqdns = [aws_route53_record.probatetree_cert_validation.fqdn]
}

# # Redirect all HTTP traffic to use HTTPS
# resource "aws_lb_listener" "http" {
#   load_balancer_arn = aws_lb.ingress.id
#   port              = 80
#   protocol          = "HTTP"
#
#   default_action {
#     type = "redirect"
#
#     redirect {
#       port        = 443
#       protocol    = "HTTPS"
#       status_code = "HTTP_301"
#     }
#   }
# }
#
# # Forward HTTPS traffic onto the container
# # Does TSL termination
# resource "aws_lb_listener" "https" {
#   load_balancer_arn = aws_lb.ingress.id
#   port              = 443
#   protocol          = "HTTPS"
#
#   ssl_policy      = "ELBSecurityPolicy-2016-08" # NOTE: Is this right?
#   certificate_arn = ""                          # TODO: This
#
#   default_action {
#     type = "forward"
#
#     target_group_arn = aws_lb_target_group.probatetree.id
#   }
# }



# >> ECS Service

resource "aws_ecs_service" "probatetree" {
  name            = "probatetree-${terraform.workspace}"
  cluster         = aws_ecs_cluster.probatetree.id
  task_definition = aws_ecs_task_definition.probatetree.arn
  launch_type     = "FARGATE"

  desired_count = 2

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
    subnets          = data.aws_subnets.public.ids
    assign_public_ip = true # TODO: Change when we have a NAT gateway in a private subnet
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
