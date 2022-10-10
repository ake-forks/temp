# >> RDS

resource "aws_db_subnet_group" "xtdb-backend" {
  name = "main"
  # TODO: Change to private or sepearte subnets entirely
  subnet_ids = data.aws_subnets.public.ids
}

locals {
  xtdb-backend-admin-username = "db_admin"
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

resource "aws_db_instance" "xtdb-backend" {
  identifier     = "probatetree-xtdb-${terraform.workspace}"
  db_name        = "ProbatePreeXTDB${terraform.workspace}"
  engine         = "postgres"
  engine_version = "14.4"
  instance_class = "db.t4g.small"

  allocated_storage     = 10
  max_allocated_storage = 100

  username = "db_user"
  password = random_password.xtdb-backend-admin.result

  db_subnet_group_name            = aws_db_subnet_group.xtdb-backend.name
  deletion_protection             = true
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  parameter_group_name = "default.mysql5.7"

  # TODO: IAM Database Auth?
  # TODO: VPC Security Groups?
  # TODO: Setup mainenance_window and backups
  # TODO: Setup monitoring

  # TODO: Remove for real production
  apply_immediately   = true
  skip_final_snapshot = true
}
