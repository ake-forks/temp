# >> RDS

resource "aws_db_subnet_group" "xtdb-backend" {
  name       = "xtdb-subnet-group_${terraform.workspace}"
  subnet_ids = data.aws_subnets.private.ids
}

locals {
  xtdb-backend-admin-username = "db_user"
  xtdb-backend-port           = 5432
}

resource "random_password" "xtdb-backend-admin" {
  length  = 32
  special = false
}

resource "aws_ssm_parameter" "xtdb-backend-admin-password" {
  name  = "XTDBBackendAdminUser_${terraform.workspace}"
  type  = "SecureString"
  value = random_password.xtdb-backend-admin.result
}

resource "aws_security_group" "database" {
  name   = "probatetree-xtdb-sg-${terraform.workspace}"
  vpc_id = data.aws_vpc.main.id

  ingress {
    protocol         = "tcp"
    from_port        = 0
    to_port          = local.xtdb-backend-port
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

resource "aws_db_instance" "xtdb-backend" {
  identifier     = "probatetree-xtdb-${terraform.workspace}"
  db_name        = "ProbatePreeXTDB${terraform.workspace}"
  engine         = "postgres"
  engine_version = "14.4"
  instance_class = "db.t4g.micro"
  port           = local.xtdb-backend-port

  allocated_storage     = 10
  max_allocated_storage = 100

  username = local.xtdb-backend-admin-username
  password = random_password.xtdb-backend-admin.result

  db_subnet_group_name            = aws_db_subnet_group.xtdb-backend.name
  deletion_protection             = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]
  vpc_security_group_ids          = [aws_security_group.database.id]

  # TODO: IAM Database Auth?
  # TODO: Setup mainenance_window and backups
  # TODO: Setup monitoring

  # TODO: Remove for real production
  apply_immediately   = true
  skip_final_snapshot = true
}
