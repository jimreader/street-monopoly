terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)
  # Determine AMI architecture from instance type
  is_arm = length(regexall("^t4g", var.ec2_instance_type)) > 0
}

# ===========================================================================
# VPC — simple public-only networking (no NAT gateway needed)
# ===========================================================================
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = { Name = "${var.app_name}-vpc" }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "${var.app_name}-igw" }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index + 1}.0/24"
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = true
  tags                    = { Name = "${var.app_name}-public-${count.index + 1}" }
}

# Private subnets for RDS only (no NAT needed — RDS doesn't need internet)
resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = local.azs[count.index]
  tags              = { Name = "${var.app_name}-private-${count.index + 1}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
  tags = { Name = "${var.app_name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ===========================================================================
# SECURITY GROUPS
# ===========================================================================
resource "aws_security_group" "ec2" {
  name_prefix = "${var.app_name}-ec2-"
  vpc_id      = aws_vpc.main.id

  # API traffic from CloudFront (and direct access during setup)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Spring Boot API"
  }

  # No SSH — use SSM Session Manager for shell access instead

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.app_name}-ec2-sg" }
}

resource "aws_security_group" "rds" {
  name_prefix = "${var.app_name}-rds-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
    description     = "Postgres from EC2"
  }

  # Optional: allow direct access from a specific IP (e.g. your home/office)
  dynamic "ingress" {
    for_each = var.db_allow_local_ip != "" ? [1] : []
    content {
      from_port   = 5432
      to_port     = 5432
      protocol    = "tcp"
      cidr_blocks = ["${var.db_allow_local_ip}/32"]
      description = "Postgres from local machine"
    }
  }

  tags = { Name = "${var.app_name}-rds-sg" }
}

# ===========================================================================
# RDS — cheapest PostgreSQL
# ===========================================================================
resource "aws_db_subnet_group" "main" {
  name = "${var.app_name}-db-subnet"
  # When local access is enabled, RDS needs public subnets to be reachable from the internet
  subnet_ids = var.db_allow_local_ip != "" ? aws_subnet.public[*].id : aws_subnet.private[*].id
  tags       = { Name = "${var.app_name}-db-subnet" }
}

resource "aws_db_instance" "postgres" {
  identifier     = "${var.app_name}-db"
  engine         = "postgres"
  engine_version = "18.3"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  storage_type          = "gp3"
  storage_encrypted     = true
  db_name               = "street_monopoly"
  username              = var.db_username
  password              = var.db_password
  db_subnet_group_name  = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az            = false
  publicly_accessible = var.db_allow_local_ip != "" ? true : false
  skip_final_snapshot = true

  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  tags = { Name = "${var.app_name}-db" }
}

# ===========================================================================
# EC2 — single instance running the Spring Boot app
# ===========================================================================

# Find the latest Amazon Linux 2023 AMI (arm64 or x86_64 depending on instance type)
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*"]
  }

  filter {
    name   = "architecture"
    values = [local.is_arm ? "arm64" : "x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# IAM role for EC2 (allows SSM Session Manager as an SSH alternative)
resource "aws_iam_role" "ec2" {
  name = "${var.app_name}-ec2-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Allow EC2 to download the JAR from S3 during deployment
resource "aws_iam_role_policy" "ec2_s3_deploy" {
  name = "${var.app_name}-ec2-s3-deploy"
  role = aws_iam_role.ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:GetObject"]
      Resource = "${aws_s3_bucket.admin_app.arn}/deploy/*"
    }]
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.app_name}-ec2-profile"
  role = aws_iam_role.ec2.name
}

resource "aws_instance" "backend" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.ec2_instance_type
  subnet_id              = aws_subnet.public[0].id
  vpc_security_group_ids = [aws_security_group.ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2.name

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  # Note: CORS origins use a wildcard initially. The deploy script updates
  # the env file with the actual CloudFront domains after first deployment.
  user_data = base64encode(templatefile("${path.module}/user_data.sh.tpl", {
    db_endpoint    = aws_db_instance.postgres.endpoint
    db_name        = aws_db_instance.postgres.db_name
    db_username    = var.db_username
    db_password    = var.db_password
    auth0_domain   = var.auth0_domain
    auth0_audience = var.auth0_audience
    mail_username  = var.mail_username
    mail_password  = var.mail_password
    admin_url      = "*"
    player_url     = "*"
  }))

  tags = { Name = "${var.app_name}-backend" }

  depends_on = [aws_db_instance.postgres]
}

# Elastic IP — gives a stable public IP and DNS independent of the instance lifecycle.
# CloudFront origins use this, breaking the circular dependency.
resource "aws_eip" "backend" {
  instance = aws_instance.backend.id
  domain   = "vpc"
  tags     = { Name = "${var.app_name}-eip" }
}

# ===========================================================================
# S3 + CLOUDFRONT — static hosting for React apps, API proxied to EC2
# ===========================================================================

# --- Admin App ---
resource "aws_s3_bucket" "admin_app" {
  bucket_prefix = "${var.app_name}-admin-"
  force_destroy = true
  tags          = { Name = "${var.app_name}-admin" }
}

resource "aws_s3_bucket_public_access_block" "admin_app" {
  bucket                  = aws_s3_bucket.admin_app.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "admin_app" {
  name                              = "${var.app_name}-admin-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "admin_app" {
  enabled             = true
  default_root_object = "index.html"
  comment             = "Road Rush Admin"

  origin {
    domain_name              = aws_s3_bucket.admin_app.bucket_regional_domain_name
    origin_id                = "s3-admin"
    origin_access_control_id = aws_cloudfront_origin_access_control.admin_app.id
  }

  # Proxy /api/* to EC2
  origin {
    domain_name = aws_eip.backend.public_dns
    origin_id   = "ec2-api"
    custom_origin_config {
      http_port              = 8080
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  ordered_cache_behavior {
    path_pattern     = "/api/*"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "ec2-api"

    forwarded_values {
      query_string = true
      headers      = ["Authorization", "Content-Type", "Origin"]
      cookies { forward = "none" }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 0
    max_ttl                = 0
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "s3-admin"
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 86400
    max_ttl     = 31536000
  }

  # SPA fallback
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
    error_caching_min_ttl = 10
  }
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
    error_caching_min_ttl = 10
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = { Name = "${var.app_name}-admin-cdn" }
}

resource "aws_s3_bucket_policy" "admin_app" {
  bucket = aws_s3_bucket.admin_app.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFront"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.admin_app.arn}/*"
      Condition = { StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.admin_app.arn } }
    }]
  })
}

# --- Player App ---
resource "aws_s3_bucket" "player_app" {
  bucket_prefix = "${var.app_name}-player-"
  force_destroy = true
  tags          = { Name = "${var.app_name}-player" }
}

resource "aws_s3_bucket_public_access_block" "player_app" {
  bucket                  = aws_s3_bucket.player_app.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "player_app" {
  name                              = "${var.app_name}-player-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "player_app" {
  enabled             = true
  default_root_object = "index.html"
  comment             = "Road Rush Player"

  origin {
    domain_name              = aws_s3_bucket.player_app.bucket_regional_domain_name
    origin_id                = "s3-player"
    origin_access_control_id = aws_cloudfront_origin_access_control.player_app.id
  }

  origin {
    domain_name = aws_eip.backend.public_dns
    origin_id   = "ec2-api"
    custom_origin_config {
      http_port              = 8080
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  ordered_cache_behavior {
    path_pattern     = "/api/*"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "ec2-api"

    forwarded_values {
      query_string = true
      headers      = ["Authorization", "Content-Type", "Origin"]
      cookies { forward = "none" }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 0
    max_ttl                = 0
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "s3-player"
    viewer_protocol_policy = "redirect-to-https"

    forwarded_values {
      query_string = false
      cookies { forward = "none" }
    }

    min_ttl     = 0
    default_ttl = 86400
    max_ttl     = 31536000
  }

  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
    error_caching_min_ttl = 10
  }
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
    error_caching_min_ttl = 10
  }

  restrictions {
    geo_restriction { restriction_type = "none" }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = { Name = "${var.app_name}-player-cdn" }
}

resource "aws_s3_bucket_policy" "player_app" {
  bucket = aws_s3_bucket.player_app.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowCloudFront"
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.player_app.arn}/*"
      Condition = { StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.player_app.arn } }
    }]
  })
}
