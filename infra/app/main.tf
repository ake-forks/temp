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
        "name"      = "webserver"
        "image"     = "${aws_ecr_repository.darbylaw.repository_url}:${var.darbylaw_docker_tag}"
        "cpu"       = 1024
        "memory"    = 2048
        "essential" = true
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
    protocol = "tcp"
    # TODO: TSL setup?
    from_port   = 8080
    to_port     = 8080
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# TODO:
# resource "aws_security_group" "alb" {
#   name   = "darbylaw-task-sg-${terraform.workspace}"
#   vpc_id = data.aws_vpc.default_vpc.id
# 
#   ingress {
#     protocol = "tcp"
#     # TODO: TSL setup?
#     from_port   = 8080
#     to_port     = 8080
#     cidr_blocks = ["0.0.0.0/0"]
#   }
# 
#   egress {
#     protocol    = "-1"
#     from_port   = 0
#     to_port     = 0
#     cidr_blocks = ["0.0.0.0/0"]
#   }
# }



# >> Load Balancer

# resource "aws_lb" "main" {
#   name               = "darbylaw-alb-${terraform.workspace}"
#   internal           = false
#   load_balancer_type = "application"
#   security_groups    = [aws_security_group.alb.id]
#   subnets            = data.aws_subnets.public.ids
# }



# >> ECS Service

resource "aws_ecs_service" "darbylaw" {
  name            = "darbylaw-${terraform.workspace}"
  cluster         = aws_ecs_cluster.darbylaw.id
  task_definition = aws_ecs_task_definition.darbylaw.arn
  launch_type     = "FARGATE"

  network_configuration {
    security_groups = [aws_security_group.task.id]
    subnets         = data.aws_subnets.public.ids
    # TODO: Remove when moved into a private subnet
    assign_public_ip = true
  }

  desired_count = 1

  # TODO: Load balancer

  # If we add autoscaling:
  # lifecycle {
  #   ignore_changes = [desired_count]
  # }
}
