# >> ECR

resource "aws_ecr_repository" "probatetree" {
  name = "probatetree-${terraform.workspace}"
}

resource "aws_ecs_cluster" "probatetree" {
  name = "probatetree-${terraform.workspace}"
}



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

resource "aws_iam_role_policy_attachment" "execution_role_policy_attachment" {
  role       = aws_iam_role.execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
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
    type = "forward"

    target_group_arn = aws_lb_target_group.probatetree.id
  }
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
