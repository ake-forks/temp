# >> Route 53


# >> Certificates

resource "aws_acm_certificate" "probatetree" {
  domain_name       = "${local.config.subdomain}.${local.config.hosted_zone_name}"
  validation_method = "DNS"

  subject_alternative_names = (
    terraform.workspace == "production"
    ? [local.config.hosted_zone_name]
    : []
  )

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_acm_certificate_validation" "probatetree" {
  certificate_arn = aws_acm_certificate.probatetree.arn
  validation_record_fqdns = [
    for record in aws_route53_record.probatetree_cert_validation
    : record.fqdn
  ]
}



# >> Records

resource "aws_route53_record" "probatetree" {
  count = terraform.workspace == "production" ? 1 : 0

  zone_id = local.config.hosted_zone_id
  name    = local.config.hosted_zone_name
  type    = "A"


  alias {
    name                   = aws_lb.ingress.dns_name
    zone_id                = aws_lb.ingress.zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "probatetree-subdomain" {
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
  # See: https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/acm_certificate#referencing-domain_validation_options-with-for_each-based-resources
  for_each = {
    for dvo in aws_acm_certificate.probatetree.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  type            = each.value.type
  zone_id         = local.config.hosted_zone_id
  ttl             = 60
}
