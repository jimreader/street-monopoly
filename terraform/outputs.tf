output "admin_app_url" {
  description = "Admin app URL"
  value       = "https://${aws_cloudfront_distribution.admin_app.domain_name}"
}

output "player_app_url" {
  description = "Player app URL"
  value       = "https://${aws_cloudfront_distribution.player_app.domain_name}"
}

output "api_direct_url" {
  description = "Direct API URL (Elastic IP, HTTP only)"
  value       = "http://${aws_eip.backend.public_ip}:8080"
}

output "ec2_public_ip" {
  description = "EC2 Elastic IP"
  value       = aws_eip.backend.public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID (for SSM Session Manager)"
  value       = aws_instance.backend.id
}

output "admin_s3_bucket" {
  description = "S3 bucket for admin app"
  value       = aws_s3_bucket.admin_app.id
}

output "player_s3_bucket" {
  description = "S3 bucket for player app"
  value       = aws_s3_bucket.player_app.id
}

output "admin_cloudfront_id" {
  description = "Admin app CloudFront distribution ID"
  value       = aws_cloudfront_distribution.admin_app.id
}

output "player_cloudfront_id" {
  description = "Player app CloudFront distribution ID"
  value       = aws_cloudfront_distribution.player_app.id
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.postgres.endpoint
}

output "db_connection_info" {
  description = "Database connection details (shown only when local access is enabled)"
  value = var.db_allow_local_ip != "" ? <<-EOT

    ==========================================
    DATABASE CONNECTION (local access enabled)
    ==========================================
    Host:     ${aws_db_instance.postgres.address}
    Port:     ${aws_db_instance.postgres.port}
    Database: ${aws_db_instance.postgres.db_name}
    Username: ${var.db_username}
    
    JDBC URL: jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}
    
    Restricted to IP: ${var.db_allow_local_ip}
    
    To disable, remove db_allow_local_ip from
    terraform.tfvars and run terraform apply.
    ==========================================
  EOT
  : "Local database access is disabled. Set db_allow_local_ip in terraform.tfvars to enable."
}

output "estimated_monthly_cost" {
  value = <<-EOT

    ==========================================
    ESTIMATED MONTHLY COST
    ==========================================
    EC2 ${var.ec2_instance_type}:   ~£3/month (or free-tier with t3.micro)
    RDS ${var.db_instance_class}:  ~£10/month (or free-tier with db.t3.micro)
    CloudFront (2 distros): ~£1/month
    S3 storage:             ~£0.02/month
    ──────────────────────────────────────────
    Total:                  ~£14/month (or ~£1 on free-tier)

    NEXT STEPS:
    1. Deploy the backend JAR (see deploy.sh)
    2. Deploy the frontend apps (see deploy.sh)
    3. Update Auth0 callback URLs to:
       ${aws_cloudfront_distribution.admin_app.domain_name}
    ==========================================
  EOT
}
