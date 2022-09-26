resource "aws_s3_bucket" "tf-state-bucket" {
  bucket = "tf-state-darbylaw"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "tf-state-bucket" {
  bucket = aws_s3_bucket.tf-state-bucket.bucket

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }

  lifecycle {
    prevent_destroy = true
  }
}

# TODO: Setup tighter controlled permissions?
resource "aws_s3_bucket_acl" "tf-state-bucket" {
  bucket = aws_s3_bucket.tf-state-bucket.bucket

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "tf-state-bucket" {
  bucket = aws_s3_bucket.tf-state-bucket.id

  versioning_configuration {
    status = "Enabled"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "tf-state-bucket" {
  bucket = aws_s3_bucket.tf-state-bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true

  lifecycle {
    prevent_destroy = true
  }
}
