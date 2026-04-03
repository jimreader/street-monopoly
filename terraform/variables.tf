variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-west-2"
}

variable "app_name" {
  description = "Application name used for resource naming"
  type        = string
  default     = "street-monopoly"
}

variable "ec2_instance_type" {
  description = "EC2 instance type. Use t3.micro for free-tier, t4g.nano for cheapest ARM"
  type        = string
  default     = "t4g.nano"
}

variable "db_instance_class" {
  description = "RDS instance class. Use db.t3.micro for free-tier, db.t4g.micro for cheapest ARM"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "streetmonopoly"
}

variable "db_password" {
  description = "PostgreSQL master password"
  type        = string
  sensitive   = true
}

variable "auth0_domain" {
  description = "Auth0 tenant domain (e.g. dev-abc123.us.auth0.com)"
  type        = string
}

variable "auth0_audience" {
  description = "Auth0 API audience / identifier"
  type        = string
  default     = "https://streetmonopoly.api"
}

variable "auth0_client_id" {
  description = "Auth0 SPA application client ID (for admin app)"
  type        = string
}

variable "mail_username" {
  description = "SMTP username for sending emails"
  type        = string
  default     = ""
}

variable "mail_password" {
  description = "SMTP password / app password"
  type        = string
  sensitive   = true
  default     = ""
}
