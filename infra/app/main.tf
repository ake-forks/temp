# >> ECR

resource "aws_ecr_repository" "darbylaw" {
  name = "darbylaw-${terraform.workspace}"
}

resource "aws_ecs_cluster" "darbylaw" {
  name = "darbylaw-${terraform.workspace}"
}



# >> Execution Role

# This role is used by the *task* to make AWS API calls
# This is stuff like pulling a container & sending logs
resource "aws_iam_role" "execution_role" {
  name = "darbylaw-exec-role-${terraform.workspace}"

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



# >> Task Definition

resource "aws_ecs_task_definition" "darbylaw" {
  family                   = "darbylaw-${terraform.workspace}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"

  cpu                = 1024
  memory             = 2048
  execution_role_arn = aws_iam_role.execution_role.arn

  container_definitions = jsonencode(
    [
      {
        name      = "webserver"
        image     = "${aws_ecr_repository.darbylaw.repository_url}:${var.darbylaw_docker_tag}"
        essential = true
        portMappings = [
          {
            protocol      = "tcp"
            containerPort = var.container_port
            hostPort      = var.container_port
          }
        ]
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
  name   = "darbylaw-task-sg-${terraform.workspace}"
  vpc_id = data.aws_vpc.default_vpc.id

  ingress {
    protocol         = "tcp"
    from_port        = var.container_port
    to_port          = var.container_port
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
  name   = "darbylaw-lb-sg-${terraform.workspace}"
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

resource "aws_lb" "main" {
  name               = "darbylaw-lb-${terraform.workspace}"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb.id]
  subnets            = data.aws_subnets.public.ids

  enable_deletion_protection = true
}

resource "aws_lb_target_group" "main" {
  name        = "darbylaw-tg-${terraform.workspace}"
  port        = var.container_port
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default_vpc.id
  target_type = "ip"

  health_check {
    healthy_threshold   = "3"
    unhealthy_threshold = "2"
    interval            = "30"
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = "3"
    path                = "/healthcheck"
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.id
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "forward"

    target_group_arn = aws_lb_target_group.main.id
  }
}

# # Redirect all HTTP traffic to use HTTPS
# resource "aws_lb_listener" "http" {
#   load_balancer_arn = aws_lb.main.id
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
#   load_balancer_arn = aws_lb.main.id
#   port              = 443
#   protocol          = "HTTPS"
# 
#   ssl_policy      = "ELBSecurityPolicy-2016-08" # NOTE: Is this right?
#   certificate_arn = ""                          # TODO: This
# 
#   default_action {
#     type = "forward"
# 
#     target_group_arn = aws_lb_target_group.main.id
#   }
# }



# >> ECS Service

resource "aws_ecs_service" "darbylaw" {
  name            = "darbylaw-${terraform.workspace}"
  cluster         = aws_ecs_cluster.darbylaw.id
  task_definition = aws_ecs_task_definition.darbylaw.arn
  launch_type     = "FARGATE"

  desired_count                     = 1
  health_check_grace_period_seconds = 30 # TODO: Time the app startup

  network_configuration {
    security_groups  = [aws_security_group.task.id]
    subnets          = data.aws_subnets.public.ids
    assign_public_ip = true # TODO: Change when we have a NAT gateway in a private subnet
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.main.id
    container_name   = "webserver"
    container_port   = var.container_port
  }

  # If we add autoscaling:
  # lifecycle {
  #   ignore_changes = [desired_count]
  # }
}
