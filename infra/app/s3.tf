# >> S3


# >> Doc Store

resource "aws_s3_bucket" "doc-store" {
  bucket = "darbylaw-docs-${terraform.workspace}"

  lifecycle {
    prevent_destroy = true
  }
}

# TODO: Setup tighter controlled permissions?
resource "aws_s3_bucket_acl" "doc-store" {
  bucket = aws_s3_bucket.doc-store.bucket
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_versioning" "doc-store" {
  bucket = aws_s3_bucket.doc-store.id

  versioning_configuration {
    status = "Enabled"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_s3_bucket_public_access_block" "doc-store" {
  bucket = aws_s3_bucket.doc-store.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true

  lifecycle {
    prevent_destroy = true
  }
}
