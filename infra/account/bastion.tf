# >> Bastion
#
# A basic EC2 instance with only ssh installed used to connect
# to other services within the account

data "aws_ami" "amazon-linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-ebs"]
  }
}

resource "aws_security_group" "ssh_in-any_out" {
  name_prefix = "ssh_in-any_out"
  vpc_id      = module.main-vpc.vpc_id

  ingress {
    protocol  = "tcp"
    from_port = 22
    to_port   = 22

    # TODO: Restrict access by IP?
    #       Not sure how we'd do that tbh :S
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "bastion" {
  ami                         = data.aws_ami.amazon-linux.id
  instance_type               = "t2.micro"
  associate_public_ip_address = true
  ebs_optimized               = false
  subnet_id                   = element(sort(data.aws_subnets.public.ids), 0)
  vpc_security_group_ids      = [aws_security_group.ssh_in-any_out.id]
  user_data = templatefile(
    "${path.module}/template/user_data.sh",
    {}
  )

  user_data_replace_on_change = true

  # Needed so we can search for this instance later
  tags = {
    Name = "bastion"
  }
}
