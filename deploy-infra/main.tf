locals {
  my_ip = "${data.http.ip.response_body}/32"
}

data "http" "ip" {
  url = "https://ifconfig.me/ip"
}

resource "aws_security_group" "this" {
  name        = "${var.name}-sg"
  description = "Security Group to Data Lakehouse POC"
  vpc_id      = var.vpc_id

  ingress {
    description = "SSH From My IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [local.my_ip]
  }

  ingress {
    description = "Kafka From My IP"
    from_port   = 9094
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = [local.my_ip]
  }

  ingress {
    description = "Schema Registry UI From My IP"
    from_port   = 8000
    to_port     = 8000
    protocol    = "tcp"
    cidr_blocks = [local.my_ip]
  }

  ingress {
    description = "Schema Registry From My IP"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = [local.my_ip]
  }

  egress {
    description = "Allow All Outbound Traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-sg"
  }
}

#resource "aws_network_interface" "this" {
#  subnet_id       = var.subnet_id
#  security_groups = [aws_security_group.this.id]
#}
#
#resource "aws_instance" "this" {
#  ami           = var.ami_id
#  instance_type = var.instance_type
#  key_name      = var.key_name
#  ebs_optimized = true
#  ebs_block_device {
#    device_name = "/dev/sda1"
#    volume_size = "100"
#  }
#  network_interface {
#    network_interface_id = aws_network_interface.this.id
#    device_index         = 0
#  }
#}
#
#resource "aws_eip" "this" {
#  instance = aws_instance.this.id
#  vpc      = true
#}
